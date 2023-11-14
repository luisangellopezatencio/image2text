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
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.text.style.BackgroundColorSpan
import android.view.ViewGroup
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.isGranted
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.io.IOException
class MainActivity : ComponentActivity() {
    var mInterstitialAd: InterstitialAd?=null


    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val adRequest = AdRequest.Builder().build()

            InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(p0: LoadAdError) {
                    mInterstitialAd = null
                }

                override fun onAdLoaded(p0: InterstitialAd) {
                    mInterstitialAd = p0
                    mInterstitialAd?.show(this@MainActivity)
                }
            })

            Image2textTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {


                    var imageUri by remember {
                        mutableStateOf<Uri?>(null)
                    }
                    val context = LocalContext.current
                    val cameraController = remember {
                        LifecycleCameraController(context)
                    }


                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                    ) {
                        Header()
                        CameraCompose(imageUri, cameraController)
                        Main(imageUri, setImageUri = {newUri -> imageUri = newUri}, cameraController)
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
fun Main(imageUri: Uri?, setImageUri:(Uri?)->Unit, cameraController: LifecycleCameraController){
    val scrollState = rememberScrollState()
    var textRec by remember {
        mutableStateOf("The extracted text will be shown here...")
    }
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)


    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
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
                    Row (
                        horizontalArrangement = Arrangement.SpaceAround
                    ){
                        IconButton(onClick = { setImageUri(null); textRec = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Icon to close")
                        }
                        Button(onClick = {
                            val image: InputImage
                            try {
                                image = InputImage.fromFilePath(context, imageUri)
                                val result = recognizer.process(image)
                                //Añadir un escuchador a la tarea
                                result.addOnSuccessListener { visionText ->
                                    textRec = visionText.text
                                }
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }) {
                            Text(text = "Extract text")
                        }
                       IconButton(onClick = {
                           val clipData = ClipData.newPlainText("text", textRec)
                           clipboardManager.setPrimaryClip(clipData)
                       }) {
                           Icon(painter = painterResource(id = R.drawable.copy), contentDescription = "Icon Copy", tint = MaterialTheme.colorScheme.primary)
                       }
                        IconButton(onClick = {
                            // Crea un intent de acción SEND
                            val intent = Intent(Intent.ACTION_SEND)
                            // Establece el tipo de datos a texto plano
                            intent.type = "text/plain"
                            // Agrega el texto como extra
                            intent.putExtra(Intent.EXTRA_TEXT, textRec)
                            // Inicia la actividad con el intent
                            context.startActivity(intent)
                        }) {
                           Icon(painter = painterResource(id = R.drawable.share), contentDescription ="Icon Share", tint = MaterialTheme.colorScheme.primary )
                        }
                    }
                    Box(modifier = Modifier
                        .background(Color.LightGray) // Puedes cambiar el color de fondo
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .verticalScroll(scrollState) // Aplica el modificador de desplazamiento
                        .padding(30.dp),

                    ){
                        Text(
                            text = textRec
                        )

                }}

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
            IconButton(onClick = {   //Take
                val file = createTempFile("imagen_", ".jpg")
                val outputDirectoy = ImageCapture.OutputFileOptions.Builder(file).build()
                val executor = ContextCompat.getMainExecutor(context)
                cameraController.takePicture(outputDirectoy, executor, object : ImageCapture.OnImageSavedCallback{
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        setImageUri(outputFileResults.savedUri)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        println()
                    }
                })
            }) {
                Icon(painter = painterResource(id = R.drawable.camera), contentDescription = "Icon camera", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraCompose(imageUri: Uri?, cameraController: LifecycleCameraController) {
    val permissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    LaunchedEffect(Unit){
        permissionState.launchPermissionRequest()
    }
    if (permissionState.status.isGranted){


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
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9F)
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