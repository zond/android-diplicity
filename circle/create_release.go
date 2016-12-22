package main

import (
	"bytes"
	"flag"
	"fmt"
	"os"
	"regexp"

	"github.com/google/go-github/github"
	"golang.org/x/oauth2"
	"google.golang.org/api/drive/v3"
)

var (
	driveConfig = &oauth2.Config{
		ClientID:     "944095575246-jq2jufr9k7s244jl9qb4nk1s36av4cd5.apps.googleusercontent.com",
		ClientSecret: "U0Okcw5_XHz8565QPRsi1Nun",
		Scopes:       []string{"https://www.googleapis.com/auth/drive"},
		RedirectURL:  "urn:ietf:wg:oauth:2.0:oob",
		Endpoint: oauth2.Endpoint{
			AuthURL:  "https://accounts.google.com/o/oauth2/auth",
			TokenURL: "https://accounts.google.com/o/oauth2/token",
		},
	}
	repoOwner   = "zond"
	repo        = "android-diplicity"
	apkMimeType = "application/vnd.android.package-archive"
	parentID    = "0B67FlKHmCU84eWVnRy1tcVUtVlk"
	apkNameReg  = regexp.MustCompile("^build-.*-.*-(.*)\\.apk$")
)

type upload struct {
	client         *drive.Service
	apk            string
	build          string
	tag            string
	shortSHA       string
	webContentLink string
	previousSHA    string
}

func (u *upload) push() error {
	old, err := u.client.Files.List().OrderBy("recency desc").PageSize(1).Q(fmt.Sprintf("'%s' in parents and trashed = false", parentID)).Do()
	if err != nil {
		return err
	}
	match := apkNameReg.FindStringSubmatch(old.Files[0].Name)
	if match == nil {
		return fmt.Errorf("Old release %q doesn't match %v", old.Files[0].Name, apkNameReg)
	}
	u.previousSHA = match[1]

	f := &drive.File{
		Name: fmt.Sprintf("build-%s-%s-%s.apk", u.build, u.tag, u.shortSHA),
		Parents: []string{
			parentID,
		},
		MimeType: apkMimeType,
	}
	f, err = u.client.Files.Create(f).Do()
	if err != nil {
		return err
	}
	input, err := os.Open(u.apk)
	if err != nil {
		return err
	}
	defer input.Close()
	f, err = u.client.Files.Update(f.Id, &drive.File{
		MimeType: apkMimeType,
	}).Media(input).Do()
	if err != nil {
		return err
	}
	u.webContentLink = fmt.Sprintf("https://drive.google.com/open?id=%s", f.Id)
	return nil
}

type release struct {
	client         *github.Client
	build          string
	tag            string
	shortSHA       string
	webContentLink string
	previousSHA    string
}

func (r *release) create() error {
	msg := ""
	typ := "commit"
	commit, _, err := r.client.Repositories.GetCommit(repoOwner, repo, r.previousSHA)
	if err != nil {
		return err
	}
	commits, _, err := r.client.Repositories.ListCommits(repoOwner, repo, &github.CommitsListOptions{
		Since: *commit.Commit.Committer.Date,
	})
	if err != nil {
		return err
	}
	buf := &bytes.Buffer{}
	fmt.Fprintf(buf, "Download [build-%s-%s-%s.apk](%s)\n\n", r.build, r.tag, r.shortSHA, r.webContentLink)
	for _, commit := range commits[1:] {
		fmt.Fprintln(buf, *commit.Commit.Message)
		fmt.Fprintf(buf, "  -- %s @ %s\n\n", *commit.Commit.Committer.Name, *commit.Commit.Committer.Date)
	}
	_, _, err = r.client.Git.CreateTag(repoOwner, repo, &github.Tag{
		Tag:     &r.tag,
		SHA:     commit.SHA,
		Message: &msg,
		Object: &github.GitObject{
			Type: &typ,
			SHA:  commit.SHA,
		},
	})
	if err != nil {
		return err
	}
	body := buf.String()
	name := fmt.Sprintf("Release %s: %s", r.tag, r.shortSHA)
	_, _, err = r.client.Repositories.CreateRelease(repoOwner, repo, &github.RepositoryRelease{
		TagName:         &r.tag,
		TargetCommitish: commit.SHA,
		Name:            &name,
		Body:            &body,
	})
	return err
}

func main() {
	apk := flag.String("apk", "", "")
	build := flag.String("build", "", "")
	shortSHA := flag.String("short_sha", "", "")
	tag := flag.String("tag", "", "")
	flag.Parse()

	if os.Getenv("GDRIVE_TOKEN") == "" {
		fmt.Println("Go to", driveConfig.AuthCodeURL(""))
		code := ""
		fmt.Scanln(&code)
		token, err := driveConfig.Exchange(oauth2.NoContext, code)
		if err != nil {
			panic(err)
		}
		fmt.Println("Refresh token is", token.RefreshToken)
		return
	}

	githubTokenSource := oauth2.StaticTokenSource(
		&oauth2.Token{AccessToken: os.Getenv("GITHUB_TOKEN")},
	)
	githubTokenClient := oauth2.NewClient(oauth2.NoContext, githubTokenSource)
	githubClient := github.NewClient(githubTokenClient)

	driveClient := driveConfig.Client(oauth2.NoContext, &oauth2.Token{
		RefreshToken: os.Getenv("GDRIVE_TOKEN"),
	})
	driveService, err := drive.New(driveClient)
	if err != nil {
		panic(err)
	}

	up := &upload{
		apk:      *apk,
		client:   driveService,
		build:    *build,
		tag:      *tag,
		shortSHA: *shortSHA,
	}
	if err := up.push(); err != nil {
		panic(err)
	}

	rel := &release{
		client:         githubClient,
		shortSHA:       *shortSHA,
		build:          *build,
		tag:            *tag,
		webContentLink: up.webContentLink,
		previousSHA:    up.previousSHA,
	}
	if err := rel.create(); err != nil {
		panic(err)
	}
}
