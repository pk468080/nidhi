package com.example.nidhi.ui.screens.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nidhi.R
import com.example.nidhi.data.model.Service
import com.example.nidhi.navigation.Routes
import com.example.nidhi.viewmodel.AuthViewModel
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Locale

/* ─────────────────────────────────────────────────────────────
   Comprehensive service catalog with pricing
   ───────────────────────────────────────────────────────────── */
val allServices = listOf(
    Service(
        name = "AC Repair",
        icon = Icons.Default.AcUnit,
        description = "Professional AC repair, installation & maintenance at your doorstep.",
        basePrice = 299,
        duration = "1–2 hrs",
        rating = 4.7f,
        reviewCount = 1240,
        category = "AC Service"
    ),
    Service(
        name = "Plumber",
        icon = Icons.Default.Plumbing,
        description = "Expert plumbing solutions — leaks, fittings, pipe work & more.",
        basePrice = 199,
        duration = "1–3 hrs",
        rating = 4.5f,
        reviewCount = 980,
        category = "Plumbing"
    ),
    Service(
        name = "Electrician",
        icon = Icons.Default.ElectricalServices,
        description = "Certified electricians for wiring, installation & fault diagnosis.",
        basePrice = 249,
        duration = "1–2 hrs",
        rating = 4.6f,
        reviewCount = 860,
        category = "Electrical"
    ),
    Service(
        name = "Cleaning",
        icon = Icons.Default.CleaningServices,
        description = "Deep cleaning services for homes, kitchens, bathrooms & offices.",
        basePrice = 399,
        duration = "2–4 hrs",
        rating = 4.8f,
        reviewCount = 2100,
        category = "Home Cleaning"
    ),
    Service(
        name = "Painting",
        icon = Icons.Default.FormatPaint,
        description = "Professional painting — interior, exterior & texture finishes.",
        basePrice = 999,
        duration = "1–3 days",
        rating = 4.5f,
        reviewCount = 540,
        category = "Painting"
    ),
    Service(
        name = "Carpenter",
        icon = Icons.Default.Handyman,
        description = "Skilled carpenters for furniture repair, installation & custom work.",
        basePrice = 299,
        duration = "1–4 hrs",
        rating = 4.4f,
        reviewCount = 730,
        category = "Carpentry"
    ),
    Service(
        name = "Pest Control",
        icon = Icons.Default.BugReport,
        description = "Effective pest control — general, termite & rodent treatments.",
        basePrice = 499,
        duration = "1–2 hrs",
        rating = 4.6f,
        reviewCount = 450,
        category = "Pest Control"
    ),
    Service(
        name = "Appliance Repair",
        icon = Icons.Default.Build,
        description = "Repair for washing machines, refrigerators, microwaves & more.",
        basePrice = 349,
        duration = "1–3 hrs",
        rating = 4.3f,
        reviewCount = 620,
        category = "Appliance Repair"
    ),
    Service(
        name = "Beauty Services",
        icon = Icons.Default.Face,
        description = "At-home beauty — haircare, makeup, facial, spa & grooming.",
        basePrice = 299,
        duration = "1–3 hrs",
        rating = 4.9f,
        reviewCount = 1870,
        category = "Beauty & Wellness"
    ),
    Service(
        name = "Tuition",
        icon = Icons.Default.School,
        description = "Home & online tuition for school, college & skill-development courses.",
        basePrice = 499,
        duration = "1 hr/session",
        rating = 4.7f,
        reviewCount = 340,
        category = "Tuition / Learning"
    )
)

/* ─────────────────────────────────────────────────────────────
   Promotional offer data
   ───────────────────────────────────────────────────────────── */
data class Offer(val title: String, val subtitle: String, val gradientColors: List<Color>)

val promotionalOffers = listOf(
    Offer(
        "20% OFF on First Booking",
        "Use code: WELCOME20",
        listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC))
    ),
    Offer(
        "Free Deep Clean",
        "Book AC service & get free cleaning",
        listOf(Color(0xFF0D47A1), Color(0xFF1976D2))
    ),
    Offer(
        "Flat ₹100 OFF",
        "On bookings above ₹999",
        listOf(Color(0xFF1B5E20), Color(0xFF388E3C))
    )
)

/* ─────────────────────────────────────────────────────────────
   HOME SCREEN
   ───────────────────────────────────────────────────────────── */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {

    val viewModel: AuthViewModel = viewModel()
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val userName = user?.displayName?.takeIf { it.isNotBlank() }
        ?: user?.email?.substringBefore("@")
        ?: "User"

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val supportedLocations = remember { listOf("New Delhi", "Mumbai", "Bengaluru", "Hyderabad") }
    var selectedLocation by rememberSaveable { mutableStateOf(supportedLocations.first()) }
    var selectedLocationIndex by rememberSaveable {
        mutableIntStateOf(supportedLocations.indexOf(selectedLocation).coerceAtLeast(0))
    }
    var isFetchingCurrentLocation by rememberSaveable { mutableStateOf(false) }

    fun fallbackToNextCity(messageResId: Int) {
        selectedLocationIndex = (selectedLocationIndex + 1) % supportedLocations.size
        selectedLocation = supportedLocations[selectedLocationIndex]
        scope.launch {
            snackbarHostState.showSnackbar(context.getString(messageResId, selectedLocation))
        }
    }

    fun fetchCurrentLocation() {
        if (isFetchingCurrentLocation) return

        isFetchingCurrentLocation = true
        scope.launch {
            snackbarHostState.showSnackbar(context.getString(com.example.nidhi.R.string.home_location_fetching))
        }

        getCurrentCityName(
            context = context,
            onSuccess = { detectedCity ->
                isFetchingCurrentLocation = false
                selectedLocation = detectedCity
                selectedLocationIndex = supportedLocations.indexOf(detectedCity).takeIf { it >= 0 } ?: selectedLocationIndex
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(com.example.nidhi.R.string.home_location_updated, detectedCity)
                    )
                }
            },
            onFailure = {
                isFetchingCurrentLocation = false
                fallbackToNextCity(com.example.nidhi.R.string.home_location_fetch_failed)
            },
            onLocationUnavailable = {
                isFetchingCurrentLocation = false
                fallbackToNextCity(com.example.nidhi.R.string.home_location_unavailable)
            }
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionResult ->
        val granted = permissionResult[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissionResult[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            fetchCurrentLocation()
        } else {
            fallbackToNextCity(com.example.nidhi.R.string.home_location_permission_denied)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {

        /* ── Hero Banner ── */
        item {
            HeroBanner(
                userName = userName,
                location = selectedLocation,
                isFetchingLocation = isFetchingCurrentLocation,
                onLocationClick = {
                    if (hasLocationPermission(context)) {
                        fetchCurrentLocation()
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
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

        /* ── Quick Action Buttons ── */
        item {
            QuickActionsRow(
                navController = navController,
                onUnavailableClick = { actionName ->
                    scope.launch {
                        snackbarHostState.showSnackbar("$actionName will be available soon")
                    }
                }
            )
        }

        /* ── Promotional Offers Carousel ── */
        item {
            SectionTitle("🎉 Special Offers")
        }
        item {
            OffersCarousel()
        }

        /* ── Service Categories Grid ── */
        item {
            SectionTitle("Our Services")
        }
        item {
            ServicesGrid(services = allServices, navController = navController)
        }

        /* ── Popular Services ── */
        item {
            SectionTitle("⭐ Popular This Week")
        }
        items(allServices.sortedByDescending { it.reviewCount }.take(5)) { service ->
            PopularServiceCard(service = service, navController = navController)
        }
    }
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    val fineLocationGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarseLocationGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fineLocationGranted || coarseLocationGranted
}

private fun getCurrentCityName(
    context: Context,
    onSuccess: (String) -> Unit,
    onFailure: () -> Unit,
    onLocationUnavailable: () -> Unit
) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.lastLocation
        .addOnSuccessListener { location ->
            if (location == null) {
                onLocationUnavailable()
                return@addOnSuccessListener
            }

            resolveCityFromCoordinates(
                context = context,
                location = location,
                onSuccess = onSuccess,
                onFailure = onFailure
            )
        }
        .addOnFailureListener {
            onLocationUnavailable()
        }
}

private fun resolveCityFromCoordinates(
    context: Context,
    location: Location,
    onSuccess: (String) -> Unit,
    onFailure: () -> Unit
) {
    if (!Geocoder.isPresent()) {
        onFailure()
        return
    }

    val geocoder = Geocoder(context, Locale.getDefault())

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        geocoder.getFromLocation(location.latitude, location.longitude, 1, object : Geocoder.GeocodeListener {
            override fun onGeocode(addresses: MutableList<Address>) {
                val city = addresses.firstOrNull()?.toCityName()
                if (city.isNullOrBlank()) onFailure() else onSuccess(city)
            }

            override fun onError(errorMessage: String?) {
                onFailure()
            }
        })
    } else {
        @Suppress("DEPRECATION")
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        val city = addresses?.firstOrNull()?.toCityName()
        if (city.isNullOrBlank()) onFailure() else onSuccess(city)
    }
}

private fun Address.toCityName(): String? {
    return locality ?: subAdminArea ?: adminArea ?: featureName
}

/* ─────────────────────────────────────────────────────────────
   Hero Banner composable
   ───────────────────────────────────────────────────────────── */
@OptIn(ExperimentalMaterial3Api::class)
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
                    listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
                )
            )
            .padding(horizontal = 20.dp)
            .padding(top = 48.dp, bottom = 28.dp)
    ) {
        Column {
            /* Top row: location + logout */
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                /* Location selector */
                Row(
                    modifier = Modifier.clickable(enabled = !isFetchingLocation, onClick = onLocationClick),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = stringResource(R.string.home_location_icon_content_description),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isFetchingLocation) {
                        Spacer(Modifier.width(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                            trackColor = Color.White.copy(alpha = 0.35f)
                        )
                    } else {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.home_location_dropdown_content_description),
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                /* Logout button */
                TextButton(onClick = onLogout) {
                    Text("Logout", color = Color.White.copy(alpha = 0.8f))
                }
            }

            Spacer(Modifier.height(16.dp))

            /* Greeting */
            Text(
                text = "Hello, $userName 👋",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "What service do you need today?",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )

            Spacer(Modifier.height(18.dp))

            /* Search bar */
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSearchClick),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF1565C0)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Search for services…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────
   Quick Actions Row
   ───────────────────────────────────────────────────────────── */
@Composable
fun QuickActionsRow(
    navController: NavController,
    onUnavailableClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickActionButton(
            icon = Icons.Default.BookOnline,
            label = "Book Now",
            containerColor = Color(0xFF1565C0)
        ) { navController.navigate(Routes.SEARCH) }

        QuickActionButton(
            icon = Icons.Default.Favorite,
            label = "Favourites",
            containerColor = Color(0xFFE53935)
        ) { onUnavailableClick("Favourites") }

        QuickActionButton(
            icon = Icons.Default.History,
            label = "History",
            containerColor = Color(0xFF43A047)
        ) { navController.navigate(Routes.BOOKINGS) }

        QuickActionButton(
            icon = Icons.Default.Percent,
            label = "Offers",
            containerColor = Color(0xFFFF8F00)
        ) { onUnavailableClick("Offers") }
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
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(containerColor.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = containerColor, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
    }
}

/* ─────────────────────────────────────────────────────────────
   Section Title
   ───────────────────────────────────────────────────────────── */
@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

/* ─────────────────────────────────────────────────────────────
   Promotional Offers Carousel
   ───────────────────────────────────────────────────────────── */
@Composable
fun OffersCarousel() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(promotionalOffers) { offer ->
            OfferBannerCard(offer)
        }
    }
}

@Composable
fun OfferBannerCard(offer: Offer) {
    Box(
        modifier = Modifier
            .width(280.dp)
            .height(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(offer.gradientColors))
            .padding(20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(
                text = offer.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = offer.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

/* ─────────────────────────────────────────────────────────────
   Services Grid (2-column, scrolling disabled — inside LazyColumn)
   ───────────────────────────────────────────────────────────── */
@Composable
fun ServicesGrid(services: List<Service>, navController: NavController) {
    /* Split into rows of 2 */
    val rows = services.chunked(2)
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                rowItems.forEach { service ->
                    ServiceCard(
                        service = service,
                        navController = navController,
                        modifier = Modifier.weight(1f)
                    )
                }
                /* Fill empty slot if odd number */
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ServiceCard(service: Service, navController: NavController, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = modifier
            .height(150.dp)
            .clickable {
                navController.navigate(
                    Routes.SERVICE_DETAILS + "/${service.name.replace(" ", "_")}"
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(Color(0xFFE3F2FD), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = service.icon,
                    contentDescription = service.name,
                    modifier = Modifier.size(30.dp),
                    tint = Color(0xFF1565C0)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = service.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (service.basePrice > 0) {
                Text(
                    text = "From ₹${service.basePrice}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF1565C0)
                )
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────
   Popular Service Card
   ───────────────────────────────────────────────────────────── */
@Composable
fun PopularServiceCard(service: Service, navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable {
                navController.navigate(
                    Routes.SERVICE_DETAILS + "/${service.name.replace(" ", "_")}"
                )
            },
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            /* Icon */
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(Color(0xFFE3F2FD), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(service.icon, contentDescription = null, tint = Color(0xFF1565C0))
            }

            Spacer(Modifier.width(14.dp))

            /* Details */
            Column(modifier = Modifier.weight(1f)) {
                Text(service.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (service.duration.isNotBlank()) {
                    Text(
                        text = service.duration,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "${service.rating} (${service.reviewCount})",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            /* Price */
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "From",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    text = "₹${service.basePrice}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF1565C0),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}