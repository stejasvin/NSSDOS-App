
package com.nssdos;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.w3c.dom.*;
import org.xml.sax.SAXException;




import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


//New Note activity
public class ActivityImport extends Activity
{
    public static final String INTENT_ACTION_SELECT_DIR =
            "com.nssdos.SELECT_DIRECTORY_ACTION";
    public static final String INTENT_ACTION_SELECT_FILE =
            "com.nssdos.SELECT_FILE_ACTION";

    //Intent parameters names constants
    public static final String startDirectoryParameter =
            "com.nssdos.directoryPath";
    public static final String returnDirectoryParameter =
            "com.nssdos.directoryPathRet";
    public static final String returnFileParameter =
            "com.nssdos.filePathRet";
    public static final String showCannotReadParameter =
            "com.nssdos.showCannotRead";


//    private DataRowAppointment dataRow = null;
//    private DataTable dataTable = null;

    // Stores names of traversed directories
    private ArrayList<String> pathDirsList = new ArrayList<String>();
    

    private List<Item> fileList = new ArrayList<Item>();
    private File path = null;
    private String chosenFile;
    //private static final int DIALOG_LOAD_FILE = 1000;

    private ArrayAdapter<Item> adapter;

    private boolean showHiddenFilesAndDirs = true;

    private boolean directoryShownIsEmpty = false;

    //Action constants
    private static int currentAction = 1;//both are same
    private static final int SELECT_DIRECTORY = 1;
    private static final int SELECT_FILE = 2;
    private static final String FTYPE = ".txt";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filebrowser);



        setInitialDirectory();
        parseDirectoryPath();
        loadFileList();
        this.createFileListAdapter();
        this.initializeButtons();
        this.initializeFileListView();
        updateCurrentDirectoryTextView();
    }

    private void setInitialDirectory() {
        Intent thisInt = this.getIntent();
//        String requestedStartDir = thisInt.getStringExtra(startDirectoryParameter);
        //TODO hard coded
        
        String requestedStartDir = "/sdcard/NSSDOS";
        if(requestedStartDir!=null && requestedStartDir.length()>0 ) {
            File tempFile = new File(requestedStartDir);
            if(tempFile.isDirectory())
                this.path = tempFile;
        }

        if(this.path==null) {//No or invalid directory supplied in intent parameter
            File temp = Environment.getExternalStorageDirectory();
            if(temp.isDirectory() && temp.canRead())
                path = Environment.getExternalStorageDirectory();
            else
                path = new File("/");
        }
    }

    private void parseDirectoryPath() {
        pathDirsList.clear();
        String pathString = path.getAbsolutePath();
        String[] parts = pathString.split("/");
        int i=0;
        while(i<parts.length) {
            pathDirsList.add(parts[i]);
            i++;
        }
    }

    private void initializeButtons() {
        Button upDirButton = (Button)this.findViewById(R.id.upDirectoryButton);
        upDirButton.setOnClickListener(
                new OnClickListener() {
                    public void onClick(View v) {
                        loadDirectoryUp();
                        loadFileList();
                        adapter.notifyDataSetChanged();
                        updateCurrentDirectoryTextView();
                    }
                });

        Button selectFolderButton = (Button)this.findViewById(R.id.selectCurrentDirectoryButton);
        if(currentAction == ActivityImport.SELECT_DIRECTORY) {
            selectFolderButton.setOnClickListener(
                    new OnClickListener() {
                        public void onClick(View v) {
                            returnDirectoryFinishActivity();
                        }
                    });
        } else
            selectFolderButton.setVisibility(View.GONE);
    }

    private void loadDirectoryUp() {
        // present directory removed from list
        String s = pathDirsList.remove(pathDirsList.size() - 1);
        // path modified to exclude present directory
        path = new File(path.toString().substring(0,
                path.toString().lastIndexOf(s)));
        //fileList = null;
        fileList.clear();
    }

    private void updateCurrentDirectoryTextView() {
        int i=0;
        String curDirString = "";
        while(i<pathDirsList.size()) {
            curDirString +=pathDirsList.get(i)+"/";
            i++;
        }
        if(pathDirsList.size()==0) {
            (this.findViewById(R.id.upDirectoryButton)).setEnabled(false);
            curDirString = "/";
        }
        else (this.findViewById(R.id.upDirectoryButton)).setEnabled(true);

        //Log.d(TAG, "Will set curr dir to:"+curDirString);
        ((Button)this.findViewById(
                R.id.upDirectoryButton)).setText(
                "Current directory:\n"+curDirString);
    }//private void updateCurrentDirectoryTextView() {

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }



    private void initializeFileListView() {
        ListView lView = (ListView)this.findViewById(R.id.fileListView);
        lView.setBackgroundColor(Color.LTGRAY);
        LinearLayout.LayoutParams lParam =
                new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
        lParam.setMargins(15, 5, 15, 5);
        lView.setAdapter(this.adapter);
        lView.setOnItemClickListener( new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                chosenFile = fileList.get(position).file;
                File sel = new File(path + "/" + chosenFile);
                if (sel.isDirectory()) {
                    if(sel.canRead()) {
                        // Adds chosen directory to list
                        pathDirsList.add(chosenFile);
                        path = new File(sel + "");
                        loadFileList();
                        adapter.notifyDataSetChanged();
                        updateCurrentDirectoryTextView();
                    } else {
                        showToast("Path does not exist or cannot be read");
                    }
                }
                // File picked or an empty directory message clicked
                else {
                    if(!directoryShownIsEmpty) {
                        returnFileFinishActivity(sel.getAbsolutePath());
                    }
                }
            }
        });
    }

    private void loadFileList() {
        try {
            path.mkdirs();
        } catch (SecurityException e) {
        }
        fileList.clear();
//        if(path.getAbsolutePath().contains("TestDOS"))
//        	Toast.makeText(ActivityImport.this, "CLICK", Toast.LENGTH_LONG).show();
        if (path.exists() && path.canRead()) {
//           	
          FilenameFilter filter = new FilenameFilter() {

              public boolean accept(File dir, String filename) {
                  File sel = new File(dir, filename);
                  return (sel.isDirectory() || filename.endsWith(FTYPE));
              }
          };

            String[] fList = path.list(filter);
//            String[] fList = path.list(null);
            this.directoryShownIsEmpty = false;
            for (int i = 0; i < fList.length; i++) {

                // Convert into file path
                File sel = new File(path, fList[i]);

                int drawableID = R.drawable.file_icon;
                boolean canRead = sel.canRead();
                // Set drawables
                if (sel.isDirectory()) {
                    if(canRead) {
                        drawableID = R.drawable.folder_icon;
                    }
                    else {
                        drawableID = R.drawable.folder_icon_light;
                    }
                }

                fileList.add(i, new Item( fList[i], drawableID, canRead));
            }
            if(fileList.size()==0) {
                this.directoryShownIsEmpty = true;
                fileList.add(0, new Item("Directory is empty", -1, true));
            }
            else {//sort non empty list
                Collections.sort(fileList, new ItemFileNameComparator());
            }
        }
    }

    private void createFileListAdapter(){
        adapter = new ArrayAdapter<Item>(this,
                android.R.layout.select_dialog_item, android.R.id.text1,
                fileList)
        {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // creates view
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view
                        .findViewById(android.R.id.text1);

                // put the image on the text view
                int drawableID = 0;
                if(fileList.get(position).icon != -1) {
                    drawableID = fileList.get(position).icon;
                }
                textView.setCompoundDrawablesWithIntrinsicBounds(
                        drawableID, 0, 0, 0);

                textView.setEllipsize(null);
                int dp3 = (int) (3 * getResources().getDisplayMetrics().density + 0.5f);
                textView.setCompoundDrawablePadding(dp3);
                textView.setBackgroundColor(Color.LTGRAY);
                return view;
            }
        };
    }

    private void returnDirectoryFinishActivity() {
        Intent retIntent = new Intent();
        retIntent.putExtra(
                returnDirectoryParameter,
                path.getAbsolutePath()
        );
        this.setResult(RESULT_OK, retIntent);
        showToast("The directory selected is : " + path.getAbsolutePath());
        this.finish();
    }

    private void returnFileFinishActivity(String filePath) {
        Intent retIntent = new Intent();
        retIntent.putExtra(
                returnFileParameter,
                filePath
        );
        this.setResult(RESULT_OK, retIntent);
        finish();
    }

    protected void restoreStateFromFreeze() {}


    // Inner classes

    private class Item {
        public String file;
        public int icon;
        public boolean canRead;

        public Item(String file, Integer icon, boolean canRead) {
            this.file = file;
            this.icon = icon;
        }

        @Override
        public String toString() {
            return file;
        }
    }


    private class ItemFileNameComparator implements Comparator<Item> {
        public int compare(Item lhs, Item rhs) {
            return lhs.file.toLowerCase().compareTo(rhs.file.toLowerCase());
        }

    }
}
