package com.example.assignmentthree;

/**
 * Created by dade on 03/02/16.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

public class PhotoAdapter extends ArrayAdapter<File> {

    Context context;
    int layoutResourceId;
    File data[] = null;

    public PhotoAdapter(Context context, int layoutResourceId, File[] data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        FileHolder holder = null;
        File file = data[position];
        PhotoWithLocation photo = null;

        try {
            FileInputStream fis = ((GalleryActivity) getContext()).openFileInput(file.getName());
            ObjectInputStream is = new ObjectInputStream(fis);
            photo = (PhotoWithLocation) is.readObject();
            is.close();
            fis.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (row == null) {
            LayoutInflater inflater = ((AppCompatActivity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new FileHolder();
            holder.imgView = (ImageView) row.findViewById(R.id.imgView);
            holder.txtFileName = (TextView) row.findViewById(R.id.labelFileName);
            holder.txtLocation = (TextView) row.findViewById(R.id.labelLocation);
            holder.btnDelete = (ImageButton) row.findViewById(R.id.btnDelete);
            row.setTag(holder);
        } else {
            holder = (FileHolder) row.getTag();
        }

        holder.imgView.setImageBitmap(fetchBitmap(photo));
        holder.txtFileName.setText(file.getName());
        holder.txtLocation.setText(fetchLocation(photo));
        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Long press an item to delete it", Toast.LENGTH_SHORT).show();
            }
        });

        return (row);
    }

    public String fetchLocation(PhotoWithLocation photo) {
        return (String.format("(%f,  %f)", photo.latitude, photo.longitude));
    }

    public Bitmap fetchBitmap(PhotoWithLocation photo) {
        int THUMBNAIL_HEIGHT = 128;
        Bitmap imageBitmap;

        imageBitmap = BitmapFactory.decodeByteArray(photo.data, 0, photo.data.length);
        int width = imageBitmap.getWidth();
        int height = imageBitmap.getHeight();
        Float ratio = (float) width / height;
        imageBitmap = Bitmap.createScaledBitmap(imageBitmap, (int) (THUMBNAIL_HEIGHT * ratio), THUMBNAIL_HEIGHT, false);

        return (imageBitmap);
    }

    static class FileHolder {
        ImageView imgView;
        TextView txtFileName;
        TextView txtLocation;
        ImageButton btnDelete;
    }
}
