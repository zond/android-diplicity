package se.oort.diplicity;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.app.AlertDialog;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.net.MalformedURLException;
import java.net.URL;

import okhttp3.HttpUrl;

public class URLEditTextPreference extends EditTextPreference {
    public URLEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public URLEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public URLEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public URLEditTextPreference(Context context) {
        super(context);
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        AlertDialog dlg = (AlertDialog)getDialog();
        View positiveButton = dlg.getButton(DialogInterface.BUTTON_POSITIVE);
        getEditText().setError(null);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Diplicity", "Clicked positive button!");
                String errorMessage = onValidate(getEditText().getText().toString());
                Log.d("Diplicity", "error message: " + errorMessage);
                if (errorMessage == null) {
                    getEditText().setError(null);
                    URLEditTextPreference.this.onClick(getDialog(), DialogInterface.BUTTON_POSITIVE);
                    getDialog().dismiss();
                } else {
                    getEditText().setError(errorMessage);
                    return;
                }
            }
        });
    }

    public String onValidate(String text) {
        HttpUrl httpUrl = HttpUrl.parse(text);
        if (httpUrl != null) {
            if (text.lastIndexOf("/") != text.length() - 1) {
                return getContext().getString(R.string.api_url_must_end_with_slash);
            }
            return null;
        } else {
            return getContext().getString(R.string.invalid_url);
        }
    }
}
