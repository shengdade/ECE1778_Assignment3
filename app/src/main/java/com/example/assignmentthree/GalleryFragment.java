package com.example.assignmentthree;

import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Created by dade on 03/02/16.
 */
public class GalleryFragment extends Fragment implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    public final String TAG = "GalleryFragment";
    private ListView photosList;
    private PhotoAdapter adp;
    private File tempPictureDir;
    private File[] fileList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View resultView = inflater.inflate(R.layout.frag_gallery, container, false);

        tempPictureDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath());
        fileList = getActivity().getFilesDir().listFiles();
        photosList = (ListView) resultView.findViewById(R.id.photoList);
        adp = new PhotoAdapter(getActivity(), R.layout.frag_gallery_item, fileList);
        photosList.setAdapter(adp);
        photosList.setOnItemLongClickListener(this);
        photosList.setOnItemClickListener(this);

        return (resultView);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final int p = position;
        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(getActivity());
        dlgAlert.setMessage("Do you want to remove this file?");
        dlgAlert.setNegativeButton("No", null);
        dlgAlert.setPositiveButton("Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        removeFile(p);
                    }
                });
        dlgAlert.setCancelable(true);
        dlgAlert.create().show();
        return true;
    }

    public void removeFile(int position) {
        try {
            File fileToDelete = fileList[position];
            boolean deleted = fileToDelete.delete();
            if (deleted) {
                fileList = getActivity().getFilesDir().listFiles();
                adp = new PhotoAdapter(getActivity(), R.layout.frag_gallery_item, fileList);
                photosList.setAdapter(adp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        PhotoWithLocation photo = null;
        File file = fileList[position];
        File tempPhoto = new File(tempPictureDir.getPath() + File.separator + "temp.jpg");
        try {
            FileInputStream fis = getActivity().openFileInput(file.getName());
            ObjectInputStream is = new ObjectInputStream(fis);
            photo = (PhotoWithLocation) is.readObject();
            is.close();
            fis.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (!tempPictureDir.exists()) {
            Log.e(TAG, "Picture directory not exist, creating Pictures folder");
            if (!tempPictureDir.mkdirs()) {
                Log.e(TAG, "Directory not created");
            }
        }

        if (photo == null) {
            Log.e(TAG, "Photo to read is null");
        } else {
            writeToFile(tempPhoto, photo.data);
        }

        Intent openImage = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse("file://" + tempPhoto.getPath());
        openImage.setDataAndType(uri, "image/jpeg");
        if (tempPhoto.exists()) {
            getActivity().startActivity(openImage);
        } else {
            Toast.makeText(this.getActivity(), "No temp photo exists", Toast.LENGTH_SHORT).show();
        }
    }

    public void writeToFile(File file, byte[] data) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "Error accessing file: " + e.getMessage());
        }
    }

//    private void removeTempPhoto() {
//        String tempPhotoPath = tempPictureDir.getPath() + File.separator + "temp.jpg";
//        Log.e(TAG, tempPhotoPath);
//        File tempPhoto = new File(tempPhotoPath);
//        if (tempPhoto.exists()) {
//            Log.e(TAG, "Temporary photo already exists");
//            try {
//                boolean deleted = tempPhoto.delete();
//                if (deleted) {
//                    Log.e(TAG, "Temporary photo deleted");
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
}