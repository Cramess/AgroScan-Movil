package com.tecsup.agroscan.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tecsup.agroscan.viewmodel.MainViewModel
import com.tecsup.agroscan.data.AnalysisResult

@Composable
fun SearchScreen(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Historial",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (viewModel.analysisHistory.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay análisis registrados", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(viewModel.analysisHistory) { analysis ->
                    HistoryCard(analysis)
                }
            }
        }
    }
}

@Composable
fun HistoryCard(analysis: AnalysisResult) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = analysis.color.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        analysis.icon, 
                        contentDescription = null, 
                        modifier = Modifier.padding(10.dp),
                        tint = analysis.color
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = analysis.plantName, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(text = analysis.date, fontSize = 12.sp, color = Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Resultado: ${analysis.description}",
                fontWeight = FontWeight.SemiBold,
                color = analysis.color,
                fontSize = 15.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = analysis.summary,
                fontSize = 13.sp,
                color = Color(0xFF6C7075),
                lineHeight = 18.sp
            )
        }
    }
}
