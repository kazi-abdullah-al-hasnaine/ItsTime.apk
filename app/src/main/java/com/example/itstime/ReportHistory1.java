package com.example.itstime;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class ReportHistory1 extends AppCompatActivity {

    CardView weeklyCard, monthlyCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report_history1);

        weeklyCard = findViewById(R.id.cardWeekly);
        monthlyCard = findViewById(R.id.cardMonthly);

        weeklyCard.setOnClickListener(v -> {
            Intent intent = new Intent(ReportHistory1.this, WeeklyReport.class);
            startActivity(intent);
        });

        monthlyCard.setOnClickListener(v -> {
            Intent intent = new Intent(ReportHistory1.this, MonthlyReport.class);
            startActivity(intent);
        });
    }
}
