package edu.strathmore.subs.smupdf;

import org.apache.cordova.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import edu.strathmore.subs.viewer.*;

/**
 * This class echoes a string called from JavaScript.
 */
@SuppressLint("SetJavaScriptEnabled")
public class SUBSMuPDFIntegration extends CordovaPlugin {

    protected static final String LOG_TAG = "SUBSMuPDFIntegration";
    private static String FILE_PREFIX = "file://";
    private static String ASSET = "android_asset";

    public static final String ANDROID_OPTIONS = "android";


    public static final class Actions
    {

        public static final String VIEW_DOCUMENT = "viewDocument";

    }

    public static final class Args
    {
        public static final String URL = "url";

        public static final String CONTENT_TYPE = "contentType";

        public static final String OPTIONS = "options";
    }


    public static final String PDF = "application/pdf";

    public static final class Result
    {
        public static final String SUPPORTED = "supported";

        public static final String STATUS = "status";

        public static final String MESSAGE = "message";

    }

    private static final int REQUEST_CODE_OPEN = 1000;

    private CallbackContext callbackContextMain;


    /**
     * Executes the request and returns PluginResult.
     *
     * @param action        The action to execute.
     * @param args          JSONArry of arguments for the plugin.
     * @param callbackId    The callback id used when calling back into JavaScript.
     * @return              A PluginResult object with a status and message.
     */
    @Override
    public boolean execute(String action, JSONArray argsArray, CallbackContext callbackContext) throws JSONException {

        JSONObject args;
        JSONObject options;
        if (argsArray.length() > 0)
        {
            args = argsArray.getJSONObject(0);
            options = args.getJSONObject(Args.OPTIONS);
        }
        else
        {
            //no arguments passed, initialize with empty JSON Objects
            args = new JSONObject();
            options = new JSONObject();
        }

        if (action.equals(Actions.VIEW_DOCUMENT))
        {
            String url = args.getString(Args.URL);
            String fileName = args.getString(Args.URL);
            String contentType = args.getString(Args.CONTENT_TYPE); 

            JSONObject androidOptions = options.getJSONObject(ANDROID_OPTIONS);


            if ( fileName.startsWith( FILE_PREFIX ) )
            {
                fileName = fileName.replace( FILE_PREFIX, "" );
            }

            fileName = fileName.startsWith("//") ? fileName : "//" + fileName;

            cordova.setActivityResultCallback (this);

            //set the callbackContext
            this.callbackContextMain = callbackContext;

            this.openPDF( fileName ,callbackContext);

            try {

                // send shown event
                JSONObject successObj = new JSONObject();
                successObj.put(Result.STATUS, PluginResult.Status.OK.ordinal());
                PluginResult result = new PluginResult(PluginResult.Status.OK,
                        successObj
                );
                // need to keep callback for close event
                result.setKeepCallback(true);
                callbackContext.sendPluginResult(result);

            }catch(Exception e){
                e.printStackTrace();
            }

        } else{
            //invalid action performed
            JSONObject errorObj = new JSONObject();
            errorObj.put(Result.STATUS,
                    PluginResult.Status.INVALID_ACTION.ordinal()
            );
            errorObj.put(Result.MESSAGE, "Invalid action '" + action + "'");
            callbackContext.error(errorObj);
        }

        return true;
    }


    /**
     * Display a MuPDF with the specified URL.
     *
     * @param path           The path to load.
     * @return               "" if ok, or error message.
     */
    public void openPDF(final String path, CallbackContext callbackContext) throws JSONException {
        try {

            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    Context context = cordova.getActivity().getApplicationContext();
                    Intent intent = new Intent(context, MuPDFActivity.class);
                    intent.setAction( Intent.ACTION_VIEW );
                    intent.addCategory(Intent.CATEGORY_EMBED);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    String fileName = path;

                    intent.setData( Uri.parse(fileName) );  

                    cordova.getActivity().startActivityForResult( intent,
                            REQUEST_CODE_OPEN
                    );

                }
            });

        } catch (android.content.ActivityNotFoundException e) {

            JSONObject errorObj = new JSONObject();
            errorObj.put(Result.STATUS, PluginResult.Status.ERROR.ordinal()
            );
            errorObj.put(Result.MESSAGE,
                    "Activity not found: " + e.getMessage()
            );
            callbackContext.error(errorObj);
        }
    }


    private String stripFileProtocol(String uriString)
    {
        if (uriString.startsWith("file://"))
            uriString = uriString.substring(7);
        return uriString;
    }


    private File getFile(String fileArg)
            throws JSONException
    {
        String filePath;
        try
        {
            CordovaResourceApi resourceApi = webView.getResourceApi();
            Uri fileUri = resourceApi.remapUri(Uri.parse(fileArg));
            filePath = this.stripFileProtocol(fileUri.toString());
        }
        catch (Exception e)
        {
            filePath = fileArg;
        }
        return new File(filePath);
    }

    private void copy(InputStream in, String fileTo) throws IOException {
        // get file to be copied from assets
        //InputStream in = this.cordova.getActivity().getAssets().open(fileFrom);
        // get file where copied too, in internal storage.
        // must be MODE_WORLD_READABLE or Android can't play it
        FileOutputStream out = this.cordova.getActivity().openFileOutput(fileTo, Context.MODE_WORLD_READABLE);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0)
            out.write(buf, 0, len);
        in.close();
        out.close();
    }

    public void initialize(CordovaInterface cordova, CordovaWebView webView)
    {
        super.initialize(cordova, webView);
    }

    public void onPause(boolean multitasking)
    {
        super.onPause(multitasking);
    }

    public void onStop() {super.onStop();}

    public void onDestroy()
    {
        super.onDestroy();
    }

    public void onReset()
    {
        super.onReset();
    }


    /**
     * Called when a previously started Activity ends
     *
     * @param requestCode The request code originally supplied to startActivityForResult(),
     *                    allowing you to identify who this result came from.
     * @param resultCode  The integer result code returned by the child activity through its setResult().
     * @param intent      An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {

        if(this.callbackContextMain == null)
            return;

        if (requestCode == REQUEST_CODE_OPEN)
        {
            try
            {
                // send closed event
                JSONObject successObj = new JSONObject();
                successObj.put(Result.STATUS,
                        PluginResult.Status.NO_RESULT.ordinal()
                );
                this.callbackContextMain.success(successObj);

            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }

            this.callbackContextMain = null;
        }

    }

}
