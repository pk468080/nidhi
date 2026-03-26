package com.example.nidhi.ui.screens.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nidhi.R
import com.example.nidhi.data.model.Service
import com.example.nidhi.navigation.Routes
import com.example.nidhi.ui.theme.*
import com.example.nidhi.viewmodel.AuthViewModel
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Locale

// ─── Theme color aliases for convenience ─────────────────────────────────────
private val HeroStart   = Color(0xFF052E16) // emerald-950
private val HeroMid     = Color(0xFF064E3B) // emerald-900
private val HeroEnd     = Color(0xFF065F46) // emerald-800
private val CardGreenBg = Color(0xFFECFDF5) // emerald-50
private val AccentGreen = Green500
private val AccentTeal  = Teal400

/* ─────────────────────────────────────────────────────────────
   Service catalog (unchanged)
   ───────────────────────────────────────────────────────────── */
val allServices = listOf(
    Service("AC Repair",        Icons.Default.AcUnit,            "Professional AC repair, installation & maintenance at your doorstep.", 299,  "1–2 hrs",       4.7f, 1240, "AC Service"),
    Service("Plumber",          Icons.Default.Plumbing,          "Expert plumbing solutions — leaks, fittings, pipe work & more.",        199,  "1–3 hrs",       4.5f,  980, "Plumbing"),
    Service("Electrician",      Icons.Default.ElectricalServices,"Certified electricians for wiring, installation & fault diagnosis.",    249,  "1–2 hrs",       4.6f,  860, "Electrical"),
    Service("Cleaning",         Icons.Default.CleaningServices,  "Deep cleaning services for homes, kitchens, bathrooms & offices.",      399,  "2–4 hrs",       4.8f, 2100, "Home Cleaning"),
    Service("Painting",         Icons.Default.FormatPaint,       "Professional painting — interior, exterior & texture finishes.",        999,  "1–3 days",      4.5f,  540, "Painting"),
    Service("Carpenter",        Icons.Default.Handyman,          "Skilled carpenters for furniture repair, installation & custom work.",   299,  "1–4 hrs",       4.4f,  730, "Carpentry"),
    Service("Pest Control",     Icons.Default.BugReport,         "Effective pest control — general, termite & rodent treatments.",        499,  "1–2 hrs",       4.6f,  450, "Pest Control"),
    Service("Appliance Repair", Icons.Default.Build,             "Repair for washing machines, refrigerators, microwaves & more.",        349,  "1–3 hrs",       4.3f,  620, "Appliance Repair"),
    Service("Beauty Services",  Icons.Default.Face,              "At-home beauty — haircare, makeup, facial, spa & grooming.",            299,  "1–3 hrs",       4.9f, 1870, "Beauty & Wellness"),
    Service("Tuition",          Icons.Default.School,            "Home & online tuition for school, college & skill-development courses.", 499, "1 hr/session",   4.7f,  340, "Tuition / Learning")
)

/* ─────────────────────────────────────────────────────────────
   Promotional offers (unchanged)
   ───────────────────────────────────────────────────────────── */
data class Offer(val title: String, val subtitle: String, val gradientColors: List<Color>)

val promotionalOffers = listOf(
    Offer("20% OFF on First Booking",      "Use code: WELCOME20",                  listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC))),
    Offer("Free Deep Clean",               "Book AC service & get free cleaning",   listOf(Color(0xFF0D47A1), Color(0xFF1976D2))),
    Offer("Flat ₹100 OFF",                 "On bookings above ₹999",               listOf(HeroStart, AccentGreen))
)

/* ─────────────────────────────────────────────────────────────
   HOME SCREEN
   ───────────────────────────────────────────────────────────── */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {

    // ── All existing logic — untouched ────────────────────────────────────────
    val viewModel: AuthViewModel = viewModel()
    val context  = LocalContext.current
    val user     = FirebaseAuth.getInstance().currentUser
    val userName = user?.displayName?.takeIf { it.isNotBlank() }
        ?: user?.email?.substringBefore("@")
        ?: "User"

    val snackbarHostState   = remember { SnackbarHostState() }
    val scope               = rememberCoroutineScope()
    val supportedLocations  = remember { listOf("New Delhi", "Mumbai", "Bengaluru", "Hyderabad") }
    var selectedLocation    by rememberSaveable { mutableStateOf(supportedLocations.first()) }
    var selectedLocationIndex by rememberSaveable {
        mutableIntStateOf(supportedLocations.indexOf(selectedLocation).coerceAtLeast(0))
    }
    var isFetchingCurrentLocation by rememberSaveable { mutableStateOf(false) }

    fun fallbackToNextCity(messageResId: Int) {
        selectedLocationIndex = (selectedLocationIndex + 1) % supportedLocations.size
        selectedLocation = supportedLocations[selectedLocationIndex]
        scope.launch { snackbarHostState.showSnackbar(context.getString(messageResId, selectedLocation)) }
    }

    fun fetchCurrentLocation() {
        if (isFetchingCurrentLocation) return
        isFetchingCurrentLocation = true
        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.home_location_fetching)) }
        getCurrentCityName(
            context = context,
            onSuccess = { detectedCity ->
                isFetchingCurrentLocation = false
                selectedLocation = detectedCity
                selectedLocationIndex = supportedLocations.indexOf(detectedCity).takeIf { it >= 0 } ?: selectedLocationIndex
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.home_location_updated, detectedCity)) }
            },
            onFailure          = { isFetchingCurrentLocation = false; fallbackToNextCity(R.string.home_location_fetch_failed) },
            onLocationUnavailable = { isFetchingCurrentLocation = false; fallbackToNextCity(R.string.home_location_unavailable) }
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) fetchCurrentLocation()
        else fallbackToNextCity(R.string.home_location_permission_denied)
    }
    // ── End existing logic ────────────────────────────────────────────────────

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color(0xFFF0FDF4) // very light green tint for the page bg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = AppSpacing.extraLarge)
        ) {

            /* ── Hero Banner ── */
            item {
                HeroBanner(
                    userName           = userName,
                    location           = selectedLocation,
                    isFetchingLocation = isFetchingCurrentLocation,
                    onLocationClick    = {
                        if (hasLocationPermission(context)) fetchCurrentLocation()
                        else locationPermissionLauncher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    },
                    onSearchClick = { navController.navigate(Routes.SEARCH) },
                    onLogout = {
                        viewModel.logout()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            /* ── Quick Actions ── */
            item {
                QuickActionsRow(
                    navController        = navController,
                    onUnavailableClick   = { name ->
                        scope.launch { snackbarHostState.showSnackbar("$name will be available soon") }
                    }
                )
            }

            /* ── Promotional Offers ── */
            item { SectionTitle("🎉 Special Offers") }
            item { OffersCarousel() }

            /* ── Services Grid ── */
            item { SectionTitle("Our Services") }
            item { ServicesGrid(services = allServices, navController = navController) }

            /* ── Popular ── */
            item { SectionTitle("⭐ Popular This Week") }
            items(allServices.sortedByDescending { it.reviewCount }.take(5)) { service ->
                PopularServiceCard(service = service, navController = navController)
            }
        }
    }
}

// ── Location helpers (unchanged) ─────────────────────────────────────────────
private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

private fun getCurrentCityName(context: Context, onSuccess: (String) -> Unit, onFailure: () -> Unit, onLocationUnavailable: () -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.lastLocation
        .addOnSuccessListener { location ->
            if (location == null) { onLocationUnavailable(); return@addOnSuccessListener }
            resolveCityFromCoordinates(context, location, onSuccess, onFailure)
        }
        .addOnFailureListener { onLocationUnavailable() }
}

private fun resolveCityFromCoordinates(context: Context, location: Location, onSuccess: (String) -> Unit, onFailure: () -> Unit) {
    if (!Geocoder.isPresent()) { onFailure(); return }
    val geocoder = Geocoder(context, Locale.getDefault())
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        geocoder.getFromLocation(location.latitude, location.longitude, 1, object : Geocoder.GeocodeListener {
            override fun onGeocode(addresses: MutableList<Address>) {
                val city = addresses.firstOrNull()?.toCityName()
                if (city.isNullOrBlank()) onFailure() else onSuccess(city)
            }
            override fun onError(errorMessage: String?) { onFailure() }
        })
    } else {
        @Suppress("DEPRECATION")
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        val city = addresses?.firstOrNull()?.toCityName()
        if (city.isNullOrBlank()) onFailure() else onSuccess(city)
    }
}

private fun Address.toCityName(): String? = locality ?: subAdminArea ?: adminArea ?: featureName

/* ─────────────────────────────────────────────────────────────
   Hero Banner — deep green gradient with floating search bar
   ───────────────────────────────────────────────────────────── */
@Composable
fun HeroBanner(
    userName: String,
    location: String,
    isFetchingLocation: Boolean,
    onLocationClick: () -> Unit,
    onSearchClick: () -> Unit,
    onLogout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(HeroStart, HeroMid, HeroEnd),
                    start  = Offset(0f, 0f),
                    end    = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
    ) {
        // Decorative blurred glow blobs
        Box(
            modifier = Modifier
                .size(220.dp)
                .offset(x = (-30).dp, y = (-40).dp)
                .background(
                    Brush.radialGradient(listOf(AccentGreen.copy(alpha = 0.25f), Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = 20.dp)
                .background(
                    Brush.radialGradient(listOf(AccentTeal.copy(alpha = 0.20f), Color.Transparent)),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.large)
                .padding(top = AppSpacing.xxxLarge, bottom = AppSpacing.extraLarge + AppSpacing.medium)
        ) {
            /* Location + Logout row */
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                /* Location chip */
                Row(
                    modifier          = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable(enabled = !isFetchingLocation, onClick = onLocationClick)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = stringResource(R.string.home_location_icon_content_description),
                        tint    = AccentTeal,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text       = location,
                        style      = MaterialTheme.typography.bodyMedium,
                        color      = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 13.sp
                    )
                    Spacer(Modifier.width(4.dp))
                    if (isFetchingLocation) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(12.dp),
                            color       = Color.White,
                            strokeWidth = 1.5.dp,
                            trackColor  = Color.White.copy(alpha = 0.3f)
                        )
                    } else {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.home_location_dropdown_content_description),
                            tint     = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                /* Logout button */
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.10f))
                        .clickable(onClick = onLogout)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        "Logout",
                        color      = Color.White.copy(alpha = 0.85f),
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(AppSpacing.large))

            /* Greeting */
            Text(
                text       = "Hello, $userName 👋",
                style      = MaterialTheme.typography.headlineSmall,
                color      = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 26.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "What service do you need today?",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.75f)
            )

            Spacer(Modifier.height(AppSpacing.large + AppSpacing.medium))

            /* Floating search bar */
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(16.dp))
                    .clickable(onClick = onSearchClick),
                shape  = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(CardGreenBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint     = AccentGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text  = "Search for services…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────
   Quick Actions Row — green-themed pill buttons
   ───────────────────────────────────────────────────────────── */
@Composable
fun QuickActionsRow(navController: NavController, onUnavailableClick: (String) -> Unit) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.default, vertical = AppSpacing.medium + 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickActionButton(icon = Icons.Default.BookOnline,  label = "Book Now",   containerColor = AccentGreen)             { navController.navigate(Routes.SEARCH) }
        QuickActionButton(icon = Icons.Default.Favorite,    label = "Favourites", containerColor = Color(0xFFE53935))        { onUnavailableClick("Favourites") }
        QuickActionButton(icon = Icons.Default.History,     label = "History",    containerColor = AccentTeal)               { navController.navigate(Routes.BOOKINGS) }
        QuickActionButton(icon = Icons.Default.Percent,     label = "Offers",     containerColor = Color(0xFFFF8F00))        { onUnavailableClick("Offers") }
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    containerColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(
                    Brush.linearGradient(
                        listOf(containerColor.copy(alpha = 0.18f), containerColor.copy(alpha = 0.08f))
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint     = containerColor,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

/* ─────────────────────────────────────────────────────────────
   Section Title
   ───────────────────────────────────────────────────────────── */
@Composable
fun SectionTitle(title: String) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(start = AppSpacing.default, end = AppSpacing.default, top = AppSpacing.large, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .background(
                    Brush.verticalGradient(listOf(AccentGreen, AccentTeal)),
                    RoundedCornerShape(2.dp)
                )
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text       = title,
            style      = MaterialTheme.typography.titleLarge,
            color      = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize   = 18.sp
        )
    }
}

/* ─────────────────────────────────────────────────────────────
   Offers Carousel
   ───────────────────────────────────────────────────────────── */
@Composable
fun OffersCarousel() {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = AppSpacing.default),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
    ) {
        items(promotionalOffers) { offer -> OfferBannerCard(offer) }
    }
}

@Composable
fun OfferBannerCard(offer: Offer) {
    Box(
        modifier = Modifier
            .width(260.dp)
            .height(110.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(offer.gradientColors))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Subtle circle decoration
        Box(
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 20.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
        )
        Column {
            Text(
                text       = offer.title,
                style      = MaterialTheme.typography.titleMedium,
                color      = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize   = 15.sp
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text     = offer.subtitle,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = Color.White,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────
   Services Grid — 2-column, green-accented cards
   ───────────────────────────────────────────────────────────── */
@Composable
fun ServicesGrid(services: List<Service>, navController: NavController) {
    val rows = services.chunked(2)
    Column(
        modifier             = Modifier.padding(horizontal = AppSpacing.default),
        verticalArrangement  = Arrangement.spacedBy(12.dp)
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { service ->
                    ServiceCard(service = service, navController = navController, modifier = Modifier.weight(1f))
                }
                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ServiceCard(service: Service, navController: NavController, modifier: Modifier = Modifier) {
    Card(
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        modifier  = modifier
            .height(130.dp)
            .clickable {
                navController.navigate(Routes.SERVICE_DETAILS + "/${service.name.replace(" ", "_")}")
            }
    ) {
        Column(
            modifier             = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment  = Alignment.CenterHorizontally,
            verticalArrangement  = Arrangement.Center
        ) {
            // Icon container with gradient ring
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        Brush.linearGradient(listOf(CardGreenBg, Color(0xFFD1FAE5))),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector      = service.icon,
                    contentDescription = service.name,
                    modifier         = Modifier.size(28.dp),
                    tint             = AccentGreen
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text       = service.name,
                style      = MaterialTheme.typography.titleSmall,
                color      = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
                fontSize   = 13.sp
            )
            if (service.basePrice > 0) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text  = "From ₹${service.basePrice}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentGreen,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────
   Popular Service Card — clean list card with green accent
   ───────────────────────────────────────────────────────────── */
@Composable
fun PopularServiceCard(service: Service, navController: NavController) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.default, vertical = 5.dp)
            .clickable {
                navController.navigate(Routes.SERVICE_DETAILS + "/${service.name.replace(" ", "_")}")
            },
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            /* Icon */
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AccentGreen, AccentTeal),
                            start  = Offset(0f, 0f),
                            end    = Offset(52f, 52f)
                        ),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    service.icon,
                    contentDescription = null,
                    tint     = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            /* Details */
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    service.name,
                    style      = MaterialTheme.typography.titleMedium,
                    color      = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp
                )
                if (service.duration.isNotBlank()) {
                    Text(
                        text  = service.duration,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        null,
                        tint     = Color(0xFFFFC107),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text  = "${service.rating}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text  = "(${service.reviewCount})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            /* Price column */
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .background(CardGreenBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text     = "From",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = AccentGreen.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                        Text(
                            text       = "₹${service.basePrice}",
                            style      = MaterialTheme.typography.titleMedium,
                            color      = AccentGreen,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 15.sp
                        )
                    }
                }
            }
        }
    }
}