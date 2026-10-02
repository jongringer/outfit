package com.example.jongringer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Entity(tableName = "outfits")
data class OutfitEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ageGroup: String,
    val situation: String,
    val top: String,
    val bottom: String,
    val outer: String,
    val shoes: String,
    val colorTone: String
)

@Dao
interface OutfitDao {
    @Query("SELECT * FROM outfits ORDER BY id DESC")
    fun getAll(): Flow<List<OutfitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(outfit: OutfitEntity)

    @Query("SELECT COUNT(*) FROM outfits")
    suspend fun count(): Int
}

@Database(entities = [OutfitEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun outfitDao(): OutfitDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jongringer_outfit.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    private val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSeedData()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OutfitCoordinatorApp(database.outfitDao())
                }
            }
        }
    }

    private fun initSeedData() {
        lifecycleScope.launch {
            if (database.outfitDao().count() == 0) {
                val seedData = listOf(
                    OutfitEntity(ageGroup = "50대+", situation = "비즈니스 캐주얼", top = "네이비 피케 셔츠", bottom = "베이지 치노 팬츠", outer = "경량 네이비 블레이저", shoes = "다크브라운 페니 로퍼", colorTone = "네이비 & 베이지"),
                    OutfitEntity(ageGroup = "50대+", situation = "주말/일상", top = "차콜 크루넥 니트", bottom = "인디고 스트레이트 진", outer = "올리브 필드 재킷", shoes = "미니멀 레더 스니커즈", colorTone = "올리브 & 인디고"),
                    OutfitEntity(ageGroup = "50대+", situation = "격식/모임", top = "화이트 드레스 셔츠", bottom = "다크그레이 울 슬랙스", outer = "차콜 울 싱글 블레이저", shoes = "블랙 옥스포드화", colorTone = "모노톤 클래식"),
                    OutfitEntity(ageGroup = "40대", situation = "비즈니스 캐주얼", top = "스카이블루 옥스포드 셔츠", bottom = "그레이 테일러드 슬랙스", outer = "체크 스포츠 재킷", shoes = "브라운 더비 슈즈", colorTone = "블루 & 그레이"),
                    OutfitEntity(ageGroup = "40대", situation = "주말/일상", top = "오트밀 롱슬리브", bottom = "스트레이트 워싱 진", outer = "스웨이드 보머 재킷", shoes = "독일군 스니커즈", colorTone = "뉴트럴 & 브라운"),
                    OutfitEntity(ageGroup = "30대", situation = "비즈니스 캐주얼", top = "세미오버핏 셔츠", bottom = "스트레이트 슬랙스", outer = "미니멀 미드 블레이저", shoes = "플랫 더비 슈즈", colorTone = "모던 미니멀")
                )
                seedData.forEach { database.outfitDao().insert(it) }
            }
        }
    }
}

@Composable
fun OutfitCoordinatorApp(dao: OutfitDao) {
    val coroutineScope = rememberCoroutineScope()
    var showCamera by remember { mutableStateOf(false) }

    if (showCamera) {
        CameraScannerScreen(
            onSaveRecommendation = { detectedTone ->
                coroutineScope.launch {
                    val newOutfit = when (detectedTone) {
                        "블루/네이비" -> OutfitEntity(
                            ageGroup = "50대+",
                            situation = "스캔 추천",
                            top = "스캔 기반 네이비 셔츠",
                            bottom = "베이지 슬랙스",
                            outer = "소프트 언컨 재킷",
                            shoes = "브라운 로퍼",
                            colorTone = "네이비 기반 조화"
                        )
                        "웜톤(레드/옐로우)" -> OutfitEntity(
                            ageGroup = "50대+",
                            situation = "스캔 추천",
                            top = "스캔 기반 베이지 니트",
                            bottom = "차콜 울 팬츠",
                            outer = "카멜 해링턴 재킷",
                            shoes = "다크브라운 더비",
                            colorTone = "어스 웜톤 밸런스"
                        )
                        "그린" -> OutfitEntity(
                            ageGroup = "50대+",
                            situation = "스캔 추천",
                            top = "스캔 기반 올리브 셔츠",
                            bottom = "아이보리 치노",
                            outer = "다크네이비 블루종",
                            shoes = "화이트 스니커즈",
                            colorTone = "보태니컬 뉴트럴"
                        )
                        else -> OutfitEntity(
                            ageGroup = "50대+",
                            situation = "스캔 추천",
                            top = "스캔 기반 모노톤 상의",
                            bottom = "딥그레이 슬랙스",
                            outer = "테일러드 싱글 재킷",
                            shoes = "블랙 더비 슈즈",
                            colorTone = "모던 모노크롬"
                        )
                    }
                    dao.insert(newOutfit)
                    showCamera = false
                }
            },
            onClose = { showCamera = false }
        )
    } else {
        OutfitListScreen(
            dao = dao,
            onOpenScanner = { showCamera = true }
        )
    }
}

@Composable
fun OutfitListScreen(
    dao: OutfitDao,
    onOpenScanner: () -> Unit
) {
    val outfits by dao.getAll().collectAsState(initial = emptyList())
    val ageGroups = listOf("전체", "30대", "40대", "50대+")
    val situations = listOf("전체", "비즈니스 캐주얼", "주말/일상", "격식/모임", "스캔 추천")

    var selectedAge by remember { mutableStateOf("50대+") }
    var selectedSituation by remember { mutableStateOf("전체") }

    val filteredList = outfits.filter { item ->
        (selectedAge == "전체" || item.ageGroup == selectedAge) &&
        (selectedSituation == "전체" || item.situation == selectedSituation)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "맞춤 코디네이터",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Button(
                onClick = onOpenScanner,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(text = "의류 스캔", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        FilterBar(title = "연령대", options = ageGroups, selected = selectedAge, onSelect = { selectedAge = it })
        Spacer(modifier = Modifier.height(8.dp))
        FilterBar(title = "상황", options = situations, selected = selectedSituation, onSelect = { selectedSituation = it })
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "조회 결과: ${filteredList.size}건",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredList, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.ageGroup} · ${item.situation}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = item.colorTone,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(6.dp))
                        ItemInfoRow(label = "아우터", value = item.outer)
                        ItemInfoRow(label = "상의", value = item.top)
                        ItemInfoRow(label = "하의", value = item.bottom)
                        ItemInfoRow(label = "신발", value = item.shoes)
                    }
                }
            }
        }
    }
}

@Composable
fun FilterBar(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column {
        Text(text = title, fontSize = 11.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(2.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(options) { text ->
                val isSelected = text == selected
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.3f))
                        .clickable { onSelect(text) }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = text,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.DarkGray,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun ItemInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    ) {
        Text(
            text = "$label:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray,
            modifier = Modifier.width(48.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun CameraScannerScreen(
    onSaveRecommendation: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var detectedColorName by remember { mutableStateOf("분석 대기 중") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraExecutor = Executors.newSingleThreadExecutor()
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            val detected = analyzeCenterColor(imageProxy)
                            imageProxy.close()
                            ContextCompat.getMainExecutor(ctx).execute {
                                detectedColorName = detected
                            }
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        runCatching {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp, start = 20.dp, end = 20.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "실시간 의류 색상 감지", color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = detectedColorName,
                        color = Color.Yellow,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text(text = "취소")
                }
                Button(
                    onClick = { onSaveRecommendation(detectedColorName) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(text = "코디 생성 및 저장")
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "카메라 권한 필요", color = Color.White)
            }
        }
    }
}

private fun analyzeCenterColor(imageProxy: ImageProxy): String {
    return runCatching {
        val bitmap = imageProxy.toBitmap() ?: return "미식별"
        val centerX = bitmap.width / 2
        val centerY = bitmap.height / 2
        val pixel = bitmap.getPixel(centerX, centerY)

        val red = AndroidColor.red(pixel)
        val green = AndroidColor.green(pixel)
        val blue = AndroidColor.blue(pixel)

        val hsv = FloatArray(3)
        AndroidColor.RGBToHSV(red, green, blue, hsv)
        val hue = hsv[0]
        val saturation = hsv[1]
        val value = hsv[2]

        when {
            value < 0.2f -> "모노톤(블랙)"
            saturation < 0.15f -> if (value > 0.8f) "모노톤(화이트)" else "모노톤(그레이)"
            hue in 0f..40f || hue in 330f..360f -> "웜톤(레드/옐로우)"
            hue in 70f..160f -> "그린"
            hue in 180f..260f -> "블루/네이비"
            else -> "모노톤"
        }
    }.getOrDefault("미식별")
}
