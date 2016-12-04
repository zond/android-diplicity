package se.oort.diplicity;

import android.app.Application;

import java.util.ArrayList;

import se.oort.diplicity.apigen.MultiContainer;
import se.oort.diplicity.apigen.SingleContainer;
import se.oort.diplicity.apigen.User;

public class App extends Application {

    public static String authToken;
    public static String baseURL;
    public static User loggedInUser;
    public static MultiContainer<VariantService.Variant> variants;


}
