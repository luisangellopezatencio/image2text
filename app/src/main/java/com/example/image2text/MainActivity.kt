package com.example.image2text

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.image2text.ui.theme.Image2textTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import android.Manifest
import android.view.ViewGroup
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.isGranted

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Image2textTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var imageUri by remember {
                        mutableStateOf<Uri?>(null)
                    }


                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                    ) {
                        Header()
                        CameraCompose(imageUri)
                        Footer(imageUri, setImageUri = {newUri -> imageUri = newUri})
                    }
                }
            }
        }
    }
}


@Composable
fun Header() {

    val languages = listOf<String>("Spa", "Eng", "xxx", "xxx", "xxx")
    var expanded by remember {
        mutableStateOf(false)
    }
    var selectedLanguage by remember {
        mutableStateOf(languages[0])
    }
    


        Row (
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 10.dp),
            horizontalArrangement = Arrangement.Center,
        ){
            Text(
                text = "image2text",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top=10.dp)
            )
            Spacer(modifier = Modifier.width(80.dp))
            Box(
                modifier = Modifier.wrapContentSize()
            ) {
                Button(onClick = { expanded = !expanded }) {
                    Text(text = "Select language")
                }
                DropdownMenu(
                    expanded = expanded,
                    //Cambiar el estado al cerrar el menu
                    onDismissRequest = { expanded = false })
                {
                    //Para cada elemento de la lista crea un menu Item
                    languages.forEach{
                            option ->
                        DropdownMenuItem(
                            text = { Text(text = option) },
                            //Al seleccionar una opcion cambia el estado del lenguaje seleccionado
                            //y cierra el menu
                            onClick = {
                                selectedLanguage = option
                                expanded = false
                                println(selectedLanguage)
                            })
                    }
                }
            }

        }



    }

@Composable
fun Footer(imageUri: Uri?, setImageUri:(Uri?)->Unit){


    val context = LocalContext.current
    val bitmap = remember {
        mutableStateOf<Bitmap?>(null)
    }
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()){
            uri: Uri? -> if (uri != null) {
        setImageUri(uri)
    }
    }
    Column (
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(all = 10.dp),
    ){
        imageUri?.let {
            if (Build.VERSION.SDK_INT < 28){
                bitmap.value = MediaStore.Images
                    .Media.getBitmap(context.contentResolver, it)
            }else{
                val source  = ImageDecoder.createSource(context.contentResolver, it)
                bitmap.value = ImageDecoder.decodeBitmap(source)
            }
            bitmap.value?.let{btm ->
                Column (horizontalAlignment = Alignment.CenterHorizontally){
                    Image(
                        bitmap =  btm.asImageBitmap(),
                        contentDescription = "image selected",
                        modifier = Modifier
                            .size(400.dp)
                            .padding(20.dp))
                    IconButton(onClick = { setImageUri(null) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Icon to close")
                    }
                }

            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(all = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(onClick = { launcher.launch("image/*") }) {
                Icon(painter = painterResource(id = R.drawable.image_picker_icon), contentDescription = "Icon Picker", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { /*TODO*/ }) {
                Icon(painter = painterResource(id = R.drawable.camera), contentDescription = "Icon camera", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraCompose(imageUri: Uri?) {
    val permissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    LaunchedEffect(Unit){
        permissionState.launchPermissionRequest()
    }
    if (permissionState.status.isGranted){
        val context = LocalContext.current
        val cameraController = remember {
            LifecycleCameraController(context)
        }
        val lifecycle = LocalLifecycleOwner.current
        cameraController.bindToLifecycle(lifecycle)
        if (imageUri == null){

            AndroidView(factory = {
                    context ->
                val PrevieView = PreviewView(context).apply{
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                }
                PrevieView.controller = cameraController

                PrevieView
            },
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9F)
            )

        }

    }else{
        Text(text = "Permiso denegado" )
    }




}

@Preview(showBackground = true)
@Composable
fun HeaderPreview() {
    Image2textTheme {
        Header()
    }
}