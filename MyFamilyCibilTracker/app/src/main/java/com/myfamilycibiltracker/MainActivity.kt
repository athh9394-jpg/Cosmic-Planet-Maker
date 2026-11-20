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
            val colors = darkColorScheme(
                primary = Color(0xFF55D6BE), onPrimary = Color(0xFF06221F),
                secondary = Color(0xFF8EA8FF), background = Color(0xFF081321),
                surface = Color(0xFF122238), onSurface = Color(0xFFEAF2FF),
                onSurfaceVariant = Color(0xFF9EB0C8)
            )
            MaterialTheme(colorScheme = colors) { TrackerApp() }
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
            Surface(color = Color(0xFF10243B)) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp)) {
                    Text("YOUR PERSONAL CREDIT SPACE", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("My Family CIBIL Tracker", color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text("Family credit health at a glance", color = Color(0xFF9EB0C8), fontSize = 13.sp)
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF102238), contentColor = MaterialTheme.colorScheme.primary) {
                listOf("Home", "Family", "History", "Loans", "EMI").forEach { item ->
                    NavigationBarItem(
                        selected = page == item, onClick = { page = item },
                        icon = { Text(when (item) { "Home" -> "⌂"; "Family" -> "♙"; "History" -> "↗"; "Loans" -> "▣"; else -> "₹" }) },
                        label = { Text(item) }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (page) {
                "Home" -> {
                    Text("Good to see you", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text("Your family credit overview", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SummaryCard("Family profiles", members.size.toString(), Modifier.weight(1f))
                        SummaryCard("Tracked loans", loans.size.toString(), Modifier.weight(1f))
                    }
                    val latest = history.groupBy { it.member }.mapValues { it.value.maxByOrNull { e -> e.date } }
                    Text("CREDIT SCORE OVERVIEW", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.5.sp)
                    if (members.isEmpty()) Text("Family tab se pehla profile add karein.")
                    members.forEach { member ->
                        val entry = latest[member.name]
                        MemberCard(member.copy(score = entry?.score ?: member.score), entry?.date)
                    }
                    Button(onClick = { page = "Family" }, modifier = Modifier.fillMaxWidth()) { Text("Add family member / score") }
                    OutlinedButton(onClick = { openCibil(context) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Official CIBIL website par report check karein")
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
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text(value, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun MemberCard(member: Member, date: Long?) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(member.name, fontWeight = FontWeight.SemiBold)
                Text(if (date == null) "No score saved yet" else "Updated: ${formatDate(date)}", fontSize = 12.sp)
            }
            Text(if (member.score == 0) "—" else member.score.toString(),
                fontSize = 23.sp, fontWeight = FontWeight.Bold)
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
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(note, { note = it }, label = { Text("Note (optional, e.g. report checked)") },
            modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            val value = score.toIntOrNull()
            if (selected.isNotBlank() && value != null && value in 300..900) {
                onScore(selected, value, note.trim()); score = ""; note = ""
            }
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
    (0 until a.length()).map { val o = a.getJSONObject(it); Member(o.getString("name"), o.optInt("score", 0)) }
} catch (_: Exception) { emptyList() }

fun writeMembers(p: android.content.SharedPreferences, list: List<Member>) {
    val a = JSONArray()
    list.forEach { a.put(JSONObject().put("name", it.name).put("score", it.score)) }
    p.edit().putString("members", a.toString()).apply()
}

fun readHistory(p: android.content.SharedPreferences): List<ScoreEntry> = try {
    val a = JSONArray(p.getString("history", "[]"))
    (0 until a.length()).map { val o = a.getJSONObject(it)
        ScoreEntry(o.getString("member"), o.getInt("score"), o.getLong("date"), o.optString("note", "")) }
} catch (_: Exception) { emptyList() }

fun writeHistory(p: android.content.SharedPreferences, list: List<ScoreEntry>) {
    val a = JSONArray()
    list.forEach { a.put(JSONObject().put("member", it.member).put("score", it.score)
        .put("date", it.date).put("note", it.note)) }
    p.edit().putString("history", a.toString()).apply()
}

fun readLoans(p: android.content.SharedPreferences): List<Loan> = try {
    val a = JSONArray(p.getString("loans", "[]"))
    (0 until a.length()).map { val o = a.getJSONObject(it)
        Loan(o.getString("member"), o.getString("lender"), o.getDouble("balance"), o.getDouble("emi")) }
} catch (_: Exception) { emptyList() }
fun writeLoans(p: android.content.SharedPreferences, list: List<Loan>) {
    val a = JSONArray()
    list.forEach { a.put(JSONObject().put("member", it.member).put("lender", it.lender)
        .put("balance", it.balance).put("emi", it.emi)) }
    p.edit().putString("loans", a.toString()).apply()
}

fun openCibil(context: Context) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.cibil.com/freecibilscore")))
}
