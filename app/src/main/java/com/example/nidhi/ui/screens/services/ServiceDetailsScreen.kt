package com.example.nidhi.ui.screens.services

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.example.nidhi.navigation.Routes
import com.example.nidhi.ui.screens.home.allServices
import com.example.nidhi.ui.theme.AppSpacing
import com.example.nidhi.ui.theme.Success
import com.example.nidhi.ui.theme.Warning

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
                            tint = if (isFavourite) MaterialTheme.colorScheme.error else LocalContentColor.current
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = AppSpacing.small) {
                Button(
                    onClick = {
                        navController.navigate(
                            Routes.BOOKING + "/$displayServiceName"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppSpacing.default),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.BookOnline, contentDescription = null)
                    Spacer(Modifier.width(AppSpacing.small))
                    Text("Book Now — ₹${packages[selectedPackageIndex].price}", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = AppSpacing.default)
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
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(horizontal = AppSpacing.default, vertical = AppSpacing.medium)
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
            .padding(AppSpacing.default)
    ) {
        Text(name, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(AppSpacing.small))
        Text(description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
        Spacer(Modifier.height(AppSpacing.medium))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.default)) {
            if (rating > 0f) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = Warning, modifier = Modifier.size(AppSpacing.medium + AppSpacing.extraSmall + AppSpacing.extraSmall))
                    Spacer(Modifier.width(AppSpacing.extraSmall))
                    Text("$rating", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    if (reviewCount > 0) Text(" ($reviewCount reviews)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (duration.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(AppSpacing.default), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(AppSpacing.extraSmall))
                    Text(duration, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            .padding(horizontal = AppSpacing.default),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium - AppSpacing.extraSmall)
    ) {
        packages.forEachIndexed { idx, pkg ->
            val isSelected = idx == selectedIndex
            val accentColor = when (pkg.name) {
                "Basic" -> Success
                "Standard" -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.tertiary
            }
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(idx) }
                    .border(
                        width = if (isSelected) AppSpacing.extraSmall / 2 else AppSpacing.extraSmall / 4,
                        color = if (isSelected) accentColor else MaterialTheme.colorScheme.outline,
                        shape = MaterialTheme.shapes.medium
                    ),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) accentColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(if (isSelected) AppSpacing.extraSmall else AppSpacing.extraSmall / 4)
            ) {
                Column(
                    modifier = Modifier.padding(AppSpacing.medium),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(pkg.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = accentColor)
                    Spacer(Modifier.height(AppSpacing.extraSmall))
                    Text("₹${pkg.price}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                    Text(pkg.duration, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            .padding(horizontal = AppSpacing.default, vertical = AppSpacing.small),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(AppSpacing.extraSmall / 2)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.default)) {
            Text("What's Included — ${pkg.name}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.height(AppSpacing.medium - AppSpacing.extraSmall))
            pkg.features.forEach { feature ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Success, modifier = Modifier.size(AppSpacing.medium + AppSpacing.extraSmall + AppSpacing.extraSmall))
                    Spacer(Modifier.width(AppSpacing.small))
                    Text(feature, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(AppSpacing.extraSmall + AppSpacing.extraSmall))
            }
        }
    }
}

@Composable
private fun PricingTransparencyNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.default, vertical = AppSpacing.extraSmall)
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f), MaterialTheme.shapes.small)
            .padding(AppSpacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Info, null, tint = Warning, modifier = Modifier.size(AppSpacing.large))
        Spacer(Modifier.width(AppSpacing.small))
        Text(
            text = "Transparent pricing — no hidden charges. Estimated total shown before booking.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ProviderCard(provider: ProviderInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.default, vertical = AppSpacing.extraSmall + AppSpacing.extraSmall / 2),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(AppSpacing.extraSmall / 2)
    ) {
        Row(
            modifier = Modifier.padding(AppSpacing.medium + AppSpacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(AppSpacing.xxxLarge)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(AppSpacing.extraLarge + AppSpacing.extraSmall))
            }
            Spacer(Modifier.width(AppSpacing.medium + AppSpacing.extraSmall))
            Column(modifier = Modifier.weight(1f)) {
                Text(provider.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Text("${provider.experience} experience · ${provider.completedJobs} jobs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, tint = Warning, modifier = Modifier.size(AppSpacing.default))
                Spacer(Modifier.width(AppSpacing.extraSmall - AppSpacing.extraSmall / 4))
                Text("${provider.rating}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
private fun ReviewCard(review: ReviewInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.default, vertical = AppSpacing.extraSmall + AppSpacing.extraSmall / 2),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(AppSpacing.extraSmall / 4)
    ) {
        Column(modifier = Modifier.padding(AppSpacing.medium + AppSpacing.extraSmall)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(review.author, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.width(AppSpacing.small))
                /* Show filled stars for whole portion + half star for .5+ fraction */
                val fullStars = review.rating.toInt()
                val hasHalfStar = (review.rating - fullStars) >= 0.5f
                val emptyStars = 5 - fullStars - if (hasHalfStar) 1 else 0
                repeat(fullStars) {
                    Icon(Icons.Default.Star, null, tint = Warning, modifier = Modifier.size(AppSpacing.medium + AppSpacing.extraSmall))
                }
                if (hasHalfStar) {
                    Icon(Icons.Default.StarHalf, null, tint = Warning, modifier = Modifier.size(AppSpacing.medium + AppSpacing.extraSmall))
                }
                repeat(emptyStars) {
                    Icon(Icons.Default.StarOutline, null, tint = Warning, modifier = Modifier.size(AppSpacing.medium + AppSpacing.extraSmall))
                }
            }
            Spacer(Modifier.height(AppSpacing.extraSmall + AppSpacing.extraSmall))
            Text(review.comment, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
