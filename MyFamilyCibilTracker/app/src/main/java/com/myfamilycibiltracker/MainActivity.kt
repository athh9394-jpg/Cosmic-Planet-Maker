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
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.pow

data class Member(val name: String, val score: Int)
data class Loan(val member: String, val lender: String, val balance: Double, val emi: Double)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { TrackerApp() } }
    }
}

@Composable
fun TrackerApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("family_tracker", Context.MODE_PRIVATE) }
    var page by remember { mutableStateOf("Home") }
    var members by remember { mutableStateOf(readMembers(prefs)) }
    var loans by remember { mutableStateOf(readLoans(prefs)) }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.primary) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Text("My Family CIBIL Tracker", color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("Free • Personal & Family", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        },
        bottomBar = {
            NavigationBar {
                listOf("Home", "Family", "Loans", "EMI").forEach { item ->
                    NavigationBarItem(
                        selected = page == item, onClick = { page = item },
                        icon = { Text(when (item) { "Home" -> "⌂"; "Family" -> "♙"; "Loans" -> "▣"; else -> "₹" }) },
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
                    Text("Family dashboard", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SummaryCard("Family profiles", members.size.toString(), Modifier.weight(1f))
                        SummaryCard("Tracked loans", loans.size.toString(), Modifier.weight(1f))
                    }
                    Text("Credit score overview", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    if (members.isEmpty()) Text("Family tab se pehla profile add karein.")
                    members.forEach { MemberCard(it) }
                    Button(onClick = { page = "Family" }, modifier = Modifier.fillMaxWidth()) { Text("Add family member") }
                    OutlinedButton(onClick = { openCibil(context) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Official CIBIL report kholein")
                    }
                }
                "Family" -> FamilyPage(members, { name, score ->
                    members = members + Member(name, score); writeMembers(prefs, members)
                }, { member ->
                    members = members.filterNot { it == member }; writeMembers(prefs, members)
                    loans = loans.filterNot { it.member == member.name }; writeLoans(prefs, loans)
                })
                "Loans" -> LoanPage(members, loans, { member, lender, balance, emi ->
                    loans = loans + Loan(member, lender, balance, emi); writeLoans(prefs, loans)
                }, { loan ->
                    loans = loans.filterNot { it == loan }; writeLoans(prefs, loans)
                })
                else -> EmiPage()
            }
            Text(
                "Important: Score is manually entered. This app does not fetch or verify official CIBIL reports. Add family information only with their permission.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
            )
        }
    }
}

@Composable
fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MemberCard(member: Member) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(member.name, fontWeight = FontWeight.SemiBold)
                Text("Manually entered score", fontSize = 12.sp)
            }
            Text(if (member.score == 0) "—" else member.score.toString(),
                fontSize = 23.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FamilyPage(members: List<Member>, onAdd: (String, Int) -> Unit, onDelete: (Member) -> Unit) {
    var name by remember { mutableStateOf("") }
    var score by remember { mutableStateOf("") }
    Text("Family profiles", fontSize = 22.sp, fontWeight = FontWeight.Bold)
    OutlinedTextField(name, { name = it }, label = { Text("Member name") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(
        score, { score = it.filter(Char::isDigit).take(3) },
        label = { Text("CIBIL score (300–900; 0 = not entered)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Button(onClick = {
        val value = score.toIntOrNull() ?: 0
        if (name.isNotBlank() && (value == 0 || value in 300..900)) {
            onAdd(name.trim(), value); name = ""; score = ""
        }
    }, modifier = Modifier.fillMaxWidth()) { Text("Save profile") }
    members.forEach { member ->
        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(member.name, fontWeight = FontWeight.SemiBold)
                    Text("Score: " + if (member.score == 0) "Not entered" else member.score.toString())
                }
                TextButton(onClick = { onDelete(member) }) { Text("Delete") }
            }
        }
    }
}

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
    val emi = if (p > 0 && n > 0) {
        if (r == 0.0) p / n else p * r * (1 + r).pow(n) / ((1 + r).pow(n) - 1)
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
