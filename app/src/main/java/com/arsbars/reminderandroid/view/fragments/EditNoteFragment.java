package com.arsbars.reminderandroid.view.fragments;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Toast;

import com.arsbars.reminderandroid.MainActivity;
import com.arsbars.reminderandroid.R;
import com.arsbars.reminderandroid.data.base.DatabaseHelper;
import com.arsbars.reminderandroid.data.galleryItem.GalleryItemsRepository;
import com.arsbars.reminderandroid.data.note.NoteRepository;
import com.arsbars.reminderandroid.data.user.User;
import com.arsbars.reminderandroid.data.user.UserRepository;
import com.arsbars.reminderandroid.view.adapters.NotePhotosArrayAdapter;
import com.arsbars.reminderandroid.viewmodels.GalleryItemViewModel;
import com.arsbars.reminderandroid.viewmodels.NoteEditViewModel;
import com.arsbars.reminderandroid.viewmodels.factory.CreateNoteViewModelFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EditNoteFragment extends Fragment {
    static final int REQUEST_IMAGE_CAPTURE = 18595;

    private long noteId;
    private NoteEditViewModel noteEditViewModel;
    private MainActivity activity;
    private User currentUser;

    private List<GalleryItemViewModel> galleryItemViewModels = new ArrayList<>();

    public List<GalleryItemViewModel> getGalleryItemViewModels() {
        return this.galleryItemViewModels;
    }

    public static EditNoteFragment newInstance(long noteId) {
        EditNoteFragment fragment = new EditNoteFragment();
        fragment.noteId = noteId;
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.edit_note_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        noteEditViewModel = ViewModelProviders
                .of(this, new CreateNoteViewModelFactory(new NoteRepository(
                        new DatabaseHelper(getContext())),
                        new GalleryItemsRepository(new DatabaseHelper(getContext()))))
                .get(NoteEditViewModel.class);

        this.activity = (MainActivity) getActivity();

        if (this.activity != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            String userName = prefs.getString(getResources().getString(R.string.user_name_preference),
                    "");

            if (userName != null) {
                UserRepository repository = new UserRepository(
                        new DatabaseHelper(activity.getApplicationContext()));
                this.currentUser = repository.getUserByName(userName);

                if (this.currentUser != null) {
                    EditText noteDescriptionText = activity.findViewById(R.id.note_description);
                    noteDescriptionText.setText(noteEditViewModel.getNoteDescription(noteId,
                            this.currentUser.getId()));

                    Button confirmButton = this.activity.findViewById(R.id.create_note_button);
                    if (noteId == 0) {
                        confirmButton.setText(getResources().getText(R.string.create));
                    } else {
                        confirmButton.setText(getResources().getText(R.string.edit));
                        this.galleryItemViewModels = noteEditViewModel.getGalleryItems(this.noteId);
                    }

                    confirmButton.setOnClickListener(v -> createNoteClicked());
                    this.activity.findViewById(R.id.cancel_note_create_button)
                            .setOnClickListener(v -> goBack());
                }
            }

            Button takePhotoButton = this.activity.findViewById(R.id.takePhotoButton);
            takePhotoButton.setOnClickListener(v -> takePhoto());
        }
        setPhotoArrayAdapter();
    }

    private void setPhotoArrayAdapter() {
        GridView photosGridView = this.activity.findViewById(R.id.photosLayout);

        if (photosGridView != null) {
            NotePhotosArrayAdapter photosAdapter = new NotePhotosArrayAdapter(this.activity,
                    R.layout.photo_item, getGalleryItemViewModels());
            photosGridView.setAdapter(photosAdapter);
        }
    }

    private void createNoteClicked() {
        String noteDescription = ((EditText)getActivity().findViewById(
                R.id.note_description))
                .getText()
                .toString();
        if (noteDescription.trim().equals("")) {
            Toast.makeText(getContext(), getString(R.string.note_create_error),
                    Toast.LENGTH_SHORT).show();
        } else {
            if (this.noteId == 0) {
                noteEditViewModel.createNote(noteDescription, this.currentUser.getId(),
                        getGalleryItemViewModels());
            } else {
                noteEditViewModel.editNote(this.noteId, noteDescription,
                        getGalleryItemViewModels());
            }
            this.activity.navigateToRoot(getString(R.string.notes),
                    NotesFragment.newInstance());
        }
    }

    private void goBack() {
        this.activity.navigateToRoot(getResources().getString(R.string.notes),
                NotesFragment.newInstance());
    }

    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(this.activity.getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                Bitmap imageBitmap = (Bitmap)extras.get("data");

                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                        .format(new Date());
                String imageFileName = "JPEG_" + timeStamp + "_";
                File storageDirectory = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

                try {
                    File image = File.createTempFile(
                            imageFileName,
                            ".jpg",
                            storageDirectory
                    );
                    FileOutputStream stream = new FileOutputStream(image);
                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);

                    GalleryItemViewModel viewModel = new GalleryItemViewModel();
                    viewModel.setImagePath(image.getAbsolutePath());
                    viewModel.setThumbnail(image.getAbsolutePath());

                    galleryItemViewModels.add(viewModel);
                    setPhotoArrayAdapter();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
