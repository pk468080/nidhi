package com.example.nidhi.ui.screens.services

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.nidhi.navigation.Routes
import com.example.nidhi.ui.screens.home.allServices

data class PricingPackage(
    val name: String,
    val price: Int,
    val duration: String,
    val features: List<String>
)

data class ProviderInfo(
    val name: String,
    val rating: Float,
    val completedJobs: Int,
    val experience: String
)

data class ReviewInfo(
    val author: String,
    val rating: Float,
    val comment: String
)

/* ─────────────────────────────────────────────────────────────
   Per-service package & provider data
   ───────────────────────────────────────────────────────────── */
private fun getPackages(serviceName: String): List<PricingPackage> = when (serviceName) {

    "AC Repair" -> listOf(
        PricingPackage("Basic", 299, "1 hr",
            listOf("AC inspection", "Filter cleaning", "Basic servicing")),
        PricingPackage("Standard", 599, "2 hrs",
            listOf("Everything in Basic", "Gas top-up check", "Cooling optimisation")),
        PricingPackage("Premium", 999, "3 hrs",
            listOf("Everything in Standard", "Full overhaul", "1-month warranty"))
    )

    "Plumber" -> listOf(
        PricingPackage("Basic", 199, "1 hr",
            listOf("Leak inspection", "Tap repair", "Basic pipe check")),
        PricingPackage("Standard", 399, "2 hrs",
            listOf("Everything in Basic", "Pipe replacement", "Water pressure fix")),
        PricingPackage("Premium", 799, "3 hrs",
            listOf("Everything in Standard", "Full bathroom fitting", "1-month warranty"))
    )

    "Electrician" -> listOf(
        PricingPackage("Basic", 249, "1 hr",
            listOf("Switch repair", "Fault diagnosis", "Basic wiring fix")),
        PricingPackage("Standard", 499, "2 hrs",
            listOf("Everything in Basic", "Fan / light installation", "MCB check")),
        PricingPackage("Premium", 899, "3 hrs",
            listOf("Everything in Standard", "Full panel inspection", "1-month warranty"))
    )

    "Cleaning" -> listOf(
        PricingPackage("Basic", 399, "2 hrs",
            listOf("Living room cleaning", "Kitchen wipe-down", "Bathroom cleaning")),
        PricingPackage("Standard", 699, "3 hrs",
            listOf("Everything in Basic", "Bedroom deep clean", "Balcony cleaning")),
        PricingPackage("Premium", 1199, "5 hrs",
            listOf("Everything in Standard", "Sofa/carpet cleaning", "Move-in/out clean"))
    )

    "Painting" -> listOf(
        PricingPackage("Basic", 999, "1 day",
            listOf("One room painting", "Basic wall prep", "Single colour")),
        PricingPackage("Standard", 2499, "2 days",
            listOf("Up to 2 rooms", "Wall putty + primer", "Choice of colours")),
        PricingPackage("Premium", 4999, "3 days",
            listOf("Full home painting", "Texture/design options", "2-year guarantee"))
    )

    "Carpenter" -> listOf(
        PricingPackage("Basic", 299, "1 hr",
            listOf("Minor furniture repair", "Hinge/latch fix", "Basic polishing")),
        PricingPackage("Standard", 699, "3 hrs",
            listOf("Everything in Basic", "Cupboard installation", "Wood staining")),
        PricingPackage("Premium", 1499, "1 day",
            listOf("Everything in Standard", "Custom furniture work", "1-month warranty"))
    )

    "Pest Control" -> listOf(
        PricingPackage("Basic", 499, "1 hr",
            listOf("General pest spray", "Kitchen & bathroom", "Safe chemicals")),
        PricingPackage("Standard", 899, "2 hrs",
            listOf("Everything in Basic", "Termite treatment", "Bed-bug treatment")),
        PricingPackage("Premium", 1499, "3 hrs",
            listOf("Everything in Standard", "Rodent control", "3-month warranty"))
    )

    "Appliance Repair" -> listOf(
        PricingPackage("Basic", 349, "1 hr",
            listOf("Diagnosis & inspection", "Minor part fix", "Basic cleaning")),
        PricingPackage("Standard", 649, "2 hrs",
            listOf("Everything in Basic", "Part replacement", "Functional testing")),
        PricingPackage("Premium", 1099, "3 hrs",
            listOf("Everything in Standard", "All appliances covered", "1-month warranty"))
    )

    "Beauty Services" -> listOf(
        PricingPackage("Basic", 299, "1 hr",
            listOf("Haircut / blowout", "Basic facial", "Eyebrow threading")),
        PricingPackage("Standard", 699, "2 hrs",
            listOf("Everything in Basic", "Hair colour / highlights", "Full face makeup")),
        PricingPackage("Premium", 1299, "3 hrs",
            listOf("Everything in Standard", "Bridal makeup", "Spa & massage")),
    )

    "Tuition" -> listOf(
        PricingPackage("Basic", 499, "1 hr",
            listOf("Single subject", "One-to-one session", "Doubt clearing")),
        PricingPackage("Standard", 1199, "5 hrs",
            listOf("Up to 3 subjects", "Weekly schedule", "Practice tests")),
        PricingPackage("Premium", 2499, "20 hrs",
            listOf("All subjects", "Study plan", "Monthly progress report"))
    )

    else -> listOf(
        PricingPackage("Basic", 299, "1 hr", listOf("Standard service")),
        PricingPackage("Standard", 599, "2 hrs", listOf("Standard service", "Extended coverage")),
        PricingPackage("Premium", 999, "3 hrs", listOf("Full service", "Warranty included"))
    )
}

private val sampleProviders = listOf(
    ProviderInfo("Rajesh Kumar", 4.8f, 520, "5 yrs"),
    ProviderInfo("Priya Sharma", 4.9f, 710, "7 yrs"),
    ProviderInfo("Amit Verma", 4.6f, 390, "3 yrs")
)

private val sampleReviews = listOf(
    ReviewInfo("Rahul M.", 5f, "Excellent service! Very professional and on time."),
    ReviewInfo("Sunita K.", 4f, "Good work done. Would book again."),
    ReviewInfo("Deepak R.", 5f, "Highly recommend. Solved the issue quickly.")
)

/* ─────────────────────────────────────────────────────────────
   SERVICE DETAILS SCREEN
   ───────────────────────────────────────────────────────────── */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailsScreen(serviceName: String, navController: NavController) {

    val displayServiceName = serviceName.replace("_", " ")
    val packages = getPackages(displayServiceName)

    /* Match against the rich service data from HomeScreen */
    val serviceInfo = allServices.find {
        it.name.equals(displayServiceName, ignoreCase = true)
    }

    var selectedPackageIndex by remember { mutableIntStateOf(1) }
    var isFavourite by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(displayServiceName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isFavourite = !isFavourite }) {
                        Icon(
                            if (isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favourite",
                            tint = if (isFavourite) Color.Red else LocalContentColor.current
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        navController.navigate(
                            Routes.BOOKING + "/$displayServiceName"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.BookOnline, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Book Now — ₹${packages[selectedPackageIndex].price}", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {

            /* ── Service summary ── */
            item {
                ServiceSummaryHeader(
                    name = displayServiceName,
                    description = serviceInfo?.description
                        ?: "Professional service at your doorstep.",
                    rating = serviceInfo?.rating ?: 4.5f,
                    reviewCount = serviceInfo?.reviewCount ?: 0,
                    duration = serviceInfo?.duration ?: ""
                )
            }

            /* ── Pricing Packages ── */
            item {
                SectionHeading("Choose a Package")
                PricingPackagesRow(
                    packages = packages,
                    selectedIndex = selectedPackageIndex,
                    onSelect = { selectedPackageIndex = it }
                )
            }

            /* ── Package features ── */
            item {
                PackageFeaturesCard(packages[selectedPackageIndex])
            }

            /* ── Pricing transparency note ── */
            item {
                PricingTransparencyNote()
            }

            /* ── Available Providers ── */
            item {
                SectionHeading("Available Providers")
            }
            items(sampleProviders.size) { idx ->
                ProviderCard(sampleProviders[idx])
            }

            /* ── Reviews ── */
            item {
                SectionHeading("Customer Reviews")
            }
            items(sampleReviews.size) { idx ->
                ReviewCard(sampleReviews[idx])
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────
   Helper composables
   ───────────────────────────────────────────────────────────── */
@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun ServiceSummaryHeader(
    name: String,
    description: String,
    rating: Float,
    reviewCount: Int,
    duration: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
            .padding(16.dp)
    ) {
        Text(name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (rating > 0f) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("$rating", fontWeight = FontWeight.SemiBold)
                    if (reviewCount > 0) Text(" ($reviewCount reviews)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            if (duration.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text(duration, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun PricingPackagesRow(
    packages: List<PricingPackage>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        packages.forEachIndexed { idx, pkg ->
            val isSelected = idx == selectedIndex
            val accentColor = when (pkg.name) {
                "Basic" -> Color(0xFF43A047)
                "Standard" -> Color(0xFF1565C0)
                else -> Color(0xFF6A1B9A)
            }
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(idx) }
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) accentColor else Color.LightGray,
                        shape = RoundedCornerShape(12.dp)
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) accentColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(pkg.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accentColor)
                    Spacer(Modifier.height(4.dp))
                    Text("₹${pkg.price}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Text(pkg.duration, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun PackageFeaturesCard(pkg: PricingPackage) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("What's Included — ${pkg.name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            pkg.features.forEach { feature ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF43A047), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(feature, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun PricingTransparencyNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(Color(0xFFFFF8E1), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Info, null, tint = Color(0xFFF9A825), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Transparent pricing — no hidden charges. Estimated total shown before booking.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF5D4037)
        )
    }
}

@Composable
private fun ProviderCard(provider: ProviderInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFE3F2FD), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = Color(0xFF1565C0), modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(provider.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("${provider.experience} experience · ${provider.completedJobs} jobs", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(3.dp))
                Text("${provider.rating}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ReviewCard(review: ReviewInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(review.author, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                /* Show filled stars for whole portion + half star for .5+ fraction */
                val fullStars = review.rating.toInt()
                val hasHalfStar = (review.rating - fullStars) >= 0.5f
                val emptyStars = 5 - fullStars - if (hasHalfStar) 1 else 0
                repeat(fullStars) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp))
                }
                if (hasHalfStar) {
                    Icon(Icons.Default.StarHalf, null, tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp))
                }
                repeat(emptyStars) {
                    Icon(Icons.Default.StarOutline, null, tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(review.comment, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
        }
    }
}