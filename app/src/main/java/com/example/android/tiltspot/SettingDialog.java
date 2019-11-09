package com.example.android.tiltspot;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatDialogFragment;

public class SettingDialog extends AppCompatDialogFragment {
    private EditText height;
    private SettingDialogListener listener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder =  new AlertDialog.Builder(getActivity());
        LayoutInflater inflater =  getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.setting_dialog,null   );

        builder.setView(view)
                .setTitle("Setting Height")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                }).setPositiveButton("Apply", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Double h = Double.parseDouble(height.getText().toString());
                listener.applyValue(h);
            }
        });
        height = view.findViewById(R.id.height);

        return builder.create();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (SettingDialogListener) activity;
        } catch (ClassCastException e) {
            throw  new ClassCastException(activity.toString() + "Must Implement SettingDialogListener");
        }
    }

    public interface SettingDialogListener{
        void applyValue(double Height);
    }
}

