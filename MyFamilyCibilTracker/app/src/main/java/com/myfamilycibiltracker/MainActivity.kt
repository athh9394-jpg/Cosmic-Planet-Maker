package com.myfamilycibiltracker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow

data class Member(val name: String, val score: Int)
data class Loan(val member: String, val lender: String, val balance: Double, val emi: Double)
data class ScoreEntry(val member: String, val score: Int, val date: Long, val note: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val colors = lightColorScheme(
                primary = Color(0xFF244A78), onPrimary = Color.White,
                primaryContainer = Color(0xFFE5EEF9), onPrimaryContainer = Color(0xFF173454),
                secondary = Color(0xFF278477), onSecondary = Color.White,
                secondaryContainer = Color(0xFFDDF3ED), onSecondaryContainer = Color(0xFF14574F),
                tertiary = Color(0xFFD18A42), background = Color(0xFFF4F6FA),
                surface = Color.White, onSurface = Color(0xFF18283B),
                surfaceVariant = Color(0xFFEDF1F6), onSurfaceVariant = Color(0xFF68788B),
                outline = Color(0xFFD7DFE8), error = Color(0xFFB94B4B)
            )
            MaterialTheme(
                colorScheme = colors,
                shapes = Shapes(
                    extraSmall = RoundedCornerShape(8.dp),
                    small = RoundedCornerShape(12.dp),
                    medium = RoundedCornerShape(18.dp),
                    large = RoundedCornerShape(24.dp),
                    extraLarge = RoundedCornerShape(28.dp)
                )
            ) { TrackerApp() }
        }
    }
}

@Composable
fun TrackerApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("family_tracker", Context.MODE_PRIVATE) }
    var page by remember { mutableStateOf("Home") }
    var members by remember { mutableStateOf(readMembers(prefs)) }
    var loans by remember { mutableStateOf(readLoans(prefs)) }
    var history by remember { mutableStateOf(readHistory(prefs)) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(color = Color.White, shadowElevation = 1.dp) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 15.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFE5EEF9)) {
                        Text("₹", Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            color = Color(0xFF244A78), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column {
                        Text("PERSONAL FINANCE", color = Color(0xFF278477), fontSize = 10.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
                        Text("Credit health", color = Color(0xFF18283B),
                            fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(shape = RoundedCornerShape(50), color = Color(0xFFEAF4F1)) {
                        Text("PRIVATE", Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            color = Color(0xFF278477), fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp)
                    }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary,
                tonalElevation = 8.dp) {
                listOf("Home", "Family", "History", "Loans", "EMI").forEach { item ->
                    NavigationBarItem(
                        selected = page == item, onClick = { page = item },
                        icon = { Text(when (item) { "Home" -> "⌂"; "Family" -> "●"; "History" -> "↗"; "Loans" -> "▤"; else -> "₹" }, fontSize = 18.sp) },
                        label = { Text(item) }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (page) {
                "Home" -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF244A78),
                        shadowElevation = 3.dp
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Text("FAMILY CREDIT SNAPSHOT", color = Color(0xFFBFD5EF),
                                fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                            Text("Your credit,\nall in one place.", color = Color.White,
                                fontSize = 27.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
                            Text("Track score updates, loans and monthly EMIs.", color = Color(0xFFE0EAF6),
                                fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF355D88)) {
                                    Column(Modifier.padding(horizontal = 13.dp, vertical = 10.dp)) {
                                        Text(members.size.toString(), color = Color.White,
                                            fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                        Text("MEMBERS", color = Color(0xFFD3E1F1), fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    }
                                }
                                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF355D88)) {
                                    Column(Modifier.padding(horizontal = 13.dp, vertical = 10.dp)) {
                                        Text(loans.size.toString(), color = Color.White,
                                            fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                        Text("ACTIVE LOANS", color = Color(0xFFD3E1F1), fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    }
                                }
                            }
                        }
                    }
                    Text("CREDIT SCORE OVERVIEW", fontWeight = FontWeight.Bold, fontSize = 11.sp,
                        color = Color(0xFF68788B), letterSpacing = 1.4.sp)
                    val latest = history.groupBy { it.member }.mapValues { it.value.maxByOrNull { e -> e.date } }
                    if (members.isEmpty()) Text("Family tab se pehla profile add karein.")
                    members.forEach { member ->
                        val entry = latest[member.name]
                        MemberCard(member.copy(score = entry?.score ?: member.score), entry?.date)
                    }
                    Button(onClick = { page = "Family" }, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(vertical = 15.dp)) {
                        Text("＋  Add family member or score", fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(onClick = { openCibil(context) }, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(vertical = 14.dp)) {
                        Text("Check report on official CIBIL website")
                    }
                }
                "Family" -> FamilyPage(members, { name ->
                    if (members.none { it.name.equals(name, ignoreCase = true) }) {
                        members = members + Member(name, 0); writeMembers(prefs, members)
                    }
                }, { member, score, note ->
                    val entry = ScoreEntry(member, score, System.currentTimeMillis(), note)
                    history = history + entry
                    writeHistory(prefs, history)
                    members = members.map { if (it.name == member) it.copy(score = score) else it }
                    writeMembers(prefs, members)
                }, { member ->
                    members = members.filterNot { it == member }; writeMembers(prefs, members)
                    loans = loans.filterNot { it.member == member.name }; writeLoans(prefs, loans)
                    history = history.filterNot { it.member == member.name }; writeHistory(prefs, history)
                })
                "History" -> HistoryPage(history)
                "Loans" -> LoanPage(members, loans, { member, lender, balance, emi ->
                    loans = loans + Loan(member, lender, balance, emi); writeLoans(prefs, loans)
                }, { loan ->
                    loans = loans.filterNot { it == loan }; writeLoans(prefs, loans)
                })
                else -> EmiPage()
            }
            Text(
                "Privacy: PAN, OTP, passwords ya full credit report is app mein enter na karein. Score manually add karein. App official CIBIL report fetch nahi karta. Family member ki permission zaroor lein.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
            )
        }
    }
}

@Composable
fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 17.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(label, color = Color(0xFF748397), fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(value, fontSize = 31.sp, fontWeight = FontWeight.Bold, color = Color(0xFF244A78))
        }
    }
}

@Composable
fun MemberCard(member: Member, date: Long?) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(member.name, fontWeight = FontWeight.SemiBold, color = Color(0xFF18283B), fontSize = 15.sp)
                Text(if (date == null) "No score saved yet" else "Updated " + formatDate(date),
                    fontSize = 11.sp, color = Color(0xFF748397))
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text(if (member.score == 0) "—" else member.score.toString(),
                    fontSize = 25.sp, fontWeight = FontWeight.Bold,
                    color = if (member.score >= 750) Color(0xFF278477) else Color(0xFF244A78))
                Text("CIBIL SCORE", fontSize = 9.sp, letterSpacing = 1.sp, color = Color(0xFF8794A5))
            }
        }
    }
}

@Composable
fun FamilyPage(members: List<Member>, onAdd: (String) -> Unit,
               onScore: (String, Int, String) -> Unit, onDelete: (Member) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selected by remember(members) { mutableStateOf(members.firstOrNull()?.name.orEmpty()) }
    var score by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val parsedScore = score.toIntOrNull()
    val scoreInvalid = score.isNotBlank() && (parsedScore == null || parsedScore !in 300..900)
    Text("Family profiles", fontSize = 22.sp, fontWeight = FontWeight.Bold)
    OutlinedTextField(name, { name = it }, label = { Text("Member name") }, modifier = Modifier.fillMaxWidth())
    Button(onClick = {
        if (name.isNotBlank() && members.none { it.name.equals(name.trim(), true) }) {
            onAdd(name.trim()); selected = name.trim(); name = ""
        }
    }, modifier = Modifier.fillMaxWidth()) { Text("Add member") }
    Divider()
    Text("Add a score update", fontWeight = FontWeight.Bold)
    if (members.isNotEmpty()) {
        var expanded by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(onClick = { expanded = true }) { Text(selected.ifBlank { "Select member" }) }
            DropdownMenu(expanded, { expanded = false }) {
                members.forEach { m -> DropdownMenuItem(text = { Text(m.name) },
                    onClick = { selected = m.name; expanded = false }) }
            }
        }
        OutlinedTextField(score, { score = it.filter(Char::isDigit).take(3) },
            label = { Text("CIBIL score (300–900)") },
            isError = scoreInvalid,
            supportingText = { if (scoreInvalid) Text("Score 300 se 900 ke beech hona chahiye.") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(note, { note = it }, label = { Text("Note (optional, e.g. report checked)") },
            modifier = Modifier.fillMaxWidth())
        Button(enabled = selected.isNotBlank() && parsedScore != null && parsedScore in 300..900,
            onClick = {
                val value = parsedScore ?: return@Button
                onScore(selected, value, note.trim()); score = ""; note = ""
            }, modifier = Modifier.fillMaxWidth()) { Text("Save score update") }
    } else Text("Pehle ek family member add karein.")
    members.forEach { member ->
        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(member.name, fontWeight = FontWeight.SemiBold)
                    Text("Latest score: " + if (member.score == 0) "Not entered" else member.score.toString())
                }
                TextButton(onClick = { onDelete(member) }) { Text("Delete") }
            }
        }
    }
}

@Composable
fun HistoryPage(history: List<ScoreEntry>) {
    Text("Score history", fontSize = 22.sp, fontWeight = FontWeight.Bold)
    if (history.isEmpty()) Text("Abhi koi score update save nahi hua.")
    history.sortedByDescending { it.date }.forEach { entry ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(entry.member, fontWeight = FontWeight.SemiBold)
                    Text(entry.score.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Text(formatDate(entry.date), fontSize = 12.sp)
                if (entry.note.isNotBlank()) Text(entry.note)
            }
        }
    }
}

fun formatDate(value: Long): String =
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(value))

@Composable
fun LoanPage(members: List<Member>, loans: List<Loan>, onAdd: (String, String, Double, Double) -> Unit,
             onDelete: (Loan) -> Unit) {
    var selected by remember(members) { mutableStateOf(members.firstOrNull()?.name.orEmpty()) }
    var lender by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var emi by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    Text("Loan tracker", fontSize = 22.sp, fontWeight = FontWeight.Bold)
    if (members.isNotEmpty()) {
        Box {
            OutlinedButton(onClick = { expanded = true }) { Text(selected.ifBlank { "Select member" }) }
            DropdownMenu(expanded, { expanded = false }) {
                members.forEach { m -> DropdownMenuItem(text = { Text(m.name) },
                    onClick = { selected = m.name; expanded = false }) }
            }
        }
    } else Text("Pehle Family tab mein profile add karein.")
    OutlinedTextField(lender, { lender = it }, label = { Text("Bank / lender") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(balance, { balance = it }, label = { Text("Outstanding amount (₹)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
    OutlinedTextField(emi, { emi = it }, label = { Text("Monthly EMI (₹)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
    Button(enabled = members.isNotEmpty(), onClick = {
        val b = balance.toDoubleOrNull(); val e = emi.toDoubleOrNull()
        if (selected.isNotBlank() && lender.isNotBlank() && b != null && e != null && b >= 0 && e >= 0) {
            onAdd(selected, lender.trim(), b, e); lender = ""; balance = ""; emi = ""
        }
    }, modifier = Modifier.fillMaxWidth()) { Text("Add loan") }
    loans.forEach { loan ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("${loan.lender} • ${loan.member}", fontWeight = FontWeight.SemiBold)
                Text("Outstanding: ₹${"%,.2f".format(loan.balance)}")
                Text("Monthly EMI: ₹${"%,.2f".format(loan.emi)}")
                TextButton(onClick = { onDelete(loan) }) { Text("Delete loan") }
            }
        }
    }
}

@Composable
fun EmiPage() {
    var principal by remember { mutableStateOf("500000") }
    var rate by remember { mutableStateOf("10") }
    var months by remember { mutableStateOf("60") }
    val p = principal.toDoubleOrNull() ?: 0.0
    val r = (rate.toDoubleOrNull() ?: 0.0) / 1200.0
    val n = months.toIntOrNull() ?: 0
    val emi = if (p > 0 && p.isFinite() && r >= 0 && r.isFinite() && n in 1..600) {
        if (r == 0.0) p / n else {
            val factor = (1 + r).pow(n)
            if (factor.isFinite() && factor > 1.0) p * r * factor / (factor - 1.0) else 0.0
        }
    } else 0.0
    Text("EMI calculator", fontSize = 22.sp, fontWeight = FontWeight.Bold)
    OutlinedTextField(principal, { principal = it }, label = { Text("Loan amount (₹)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
    OutlinedTextField(rate, { rate = it }, label = { Text("Annual interest rate (%)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
    OutlinedTextField(months, { months = it }, label = { Text("Tenure (months)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Estimated monthly EMI")
            Text("₹${"%,.2f".format(emi)}", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Estimate only; lender fees and terms can change the actual EMI.", fontSize = 12.sp)
        }
    }
}

fun readMembers(p: android.content.SharedPreferences): List<Member> = try {
    val a = JSONArray(p.getString("members", "[]"))
    (0 until a.length()).mapNotNull { i ->
        runCatching {
            val o = a.getJSONObject(i)
            val name = o.optString("name", "").trim()
            val score = o.optInt("score", 0)
            if (name.isBlank() || score !in 0..900) null else Member(name, score)
        }.getOrNull()
    }.distinctBy { it.name.lowercase(Locale.ROOT) }
} catch (_: Exception) { emptyList() }

fun writeMembers(p: android.content.SharedPreferences, list: List<Member>) {
    val a = JSONArray()
    list.forEach { a.put(JSONObject().put("name", it.name).put("score", it.score)) }
    p.edit().putString("members", a.toString()).apply()
}

fun readHistory(p: android.content.SharedPreferences): List<ScoreEntry> = try {
    val a = JSONArray(p.getString("history", "[]"))
    (0 until a.length()).mapNotNull { i ->
        runCatching {
            val o = a.getJSONObject(i)
            val member = o.optString("member", "").trim()
            val score = o.optInt("score", 0)
            val date = o.optLong("date", 0L)
            if (member.isBlank() || score !in 300..900 || date <= 0L) null
            else ScoreEntry(member, score, date, o.optString("note", ""))
        }.getOrNull()
    }
} catch (_: Exception) { emptyList() }

fun writeHistory(p: android.content.SharedPreferences, list: List<ScoreEntry>) {
    val a = JSONArray()
    list.forEach { a.put(JSONObject().put("member", it.member).put("score", it.score)
        .put("date", it.date).put("note", it.note)) }
    p.edit().putString("history", a.toString()).apply()
}

fun readLoans(p: android.content.SharedPreferences): List<Loan> = try {
    val a = JSONArray(p.getString("loans", "[]"))
    (0 until a.length()).mapNotNull { i ->
        runCatching {
            val o = a.getJSONObject(i)
            val member = o.optString("member", "").trim()
            val lender = o.optString("lender", "").trim()
            val balance = o.optDouble("balance", Double.NaN)
            val emi = o.optDouble("emi", Double.NaN)
            if (member.isBlank() || lender.isBlank() || !balance.isFinite() || !emi.isFinite()
                || balance < 0 || emi < 0) null
            else Loan(member, lender, balance, emi)
        }.getOrNull()
    }
} catch (_: Exception) { emptyList() }
fun writeLoans(p: android.content.SharedPreferences, list: List<Loan>) {
    val a = JSONArray()
    list.forEach { a.put(JSONObject().put("member", it.member).put("lender", it.lender)
        .put("balance", it.balance).put("emi", it.emi)) }
    p.edit().putString("loans", a.toString()).apply()
}

fun openCibil(context: Context) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.cibil.com/freecibilscore")))
    } catch (_: android.content.ActivityNotFoundException) {
        android.widget.Toast.makeText(context, "Browser app nahi mila.", android.widget.Toast.LENGTH_SHORT).show()
    }
}
