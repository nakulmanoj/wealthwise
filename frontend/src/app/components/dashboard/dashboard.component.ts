import { Component, ElementRef, inject, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../services/api.service';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  apiService = inject(ApiService);
  dashboardData: any = null;
  loading = true;

  @ViewChild('cashFlowCanvas') cashFlowCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('categoryCanvas') categoryCanvas!: ElementRef<HTMLCanvasElement>;

  cashFlowChart: any;
  categoryChart: any;

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.apiService.getDashboardData().subscribe({
      next: (data) => {
        this.dashboardData = data;
        this.loading = false;
        // Use 100ms timeout so Angular's *ngIf block fully renders
        // and @ViewChild references are resolved before we access them
        setTimeout(() => this.renderCharts(), 100);
      },
      error: (err) => {
        console.error('Error fetching dashboard data:', err);
        this.loading = false;
      }
    });
  }

  renderCharts(): void {
    if (!this.dashboardData) return;

    // ── Cash Flow Grouped Bar Chart ───────────────────────────────────────
    if (this.cashFlowCanvas?.nativeElement) {
      if (this.cashFlowChart) this.cashFlowChart.destroy();

      const labels   = this.dashboardData.monthlyCashFlows.map((cf: any) => cf.label);
      const incomes  = this.dashboardData.monthlyCashFlows.map((cf: any) => Number(cf.income));
      const expenses = this.dashboardData.monthlyCashFlows.map((cf: any) => Number(cf.expense));

      const ctxCF = this.cashFlowCanvas.nativeElement.getContext('2d');
      if (ctxCF) {
        this.cashFlowChart = new Chart(ctxCF, {
          type: 'bar',
          data: {
            labels,
            datasets: [
              {
                label: 'Income',
                data: incomes,
                backgroundColor: '#10b981',
                borderRadius: 6,
                borderSkipped: false,
                barPercentage: 0.8,
                categoryPercentage: 0.6
              },
              {
                label: 'Expense',
                data: expenses,
                backgroundColor: '#ef4444',
                borderRadius: 6,
                borderSkipped: false,
                barPercentage: 0.8,
                categoryPercentage: 0.6
              }
            ]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
              legend: { labels: { color: '#94a3b8', font: { family: 'Inter', weight: 'bold' } } },
              tooltip: {
                callbacks: {
                  label: (ctx) => {
                    const yVal = ctx.parsed.y !== null && ctx.parsed.y !== undefined ? ctx.parsed.y : 0;
                    return ` ${ctx.dataset.label}: ₹${yVal.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;
                  }
                }
              }
            },
            scales: {
              x: {
                grid: { display: false },
                ticks: { color: '#94a3b8', font: { family: 'Inter' } }
              },
              y: {
                grid: { color: 'rgba(255,255,255,0.05)' },
                ticks: {
                  color: '#94a3b8',
                  font: { family: 'Inter' },
                  callback: (v) => '₹' + Number(v).toLocaleString('en-IN')
                }
              }
            }
          }
        });
      }
    }

    // ── Category Doughnut Chart ───────────────────────────────────────────
    if (this.categoryCanvas?.nativeElement) {
      if (this.categoryChart) this.categoryChart.destroy();

      const catLabels  = this.dashboardData.categoryDistribution.map((cd: any) => cd.categoryName);
      const catAmounts = this.dashboardData.categoryDistribution.map((cd: any) => Number(cd.amount));

      const ctxCat = this.categoryCanvas.nativeElement.getContext('2d');
      if (ctxCat) {
        this.categoryChart = new Chart(ctxCat, {
          type: 'doughnut',
          data: {
            labels: catLabels,
            datasets: [{
              data: catAmounts,
              backgroundColor: [
                '#06b6d4', '#8b5cf6', '#ec4899', '#f59e0b',
                '#10b981', '#ef4444', '#3b82f6', '#64748b'
              ],
              borderWidth: 1,
              borderColor: 'rgba(255,255,255,0.1)'
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
              legend: {
                position: 'right',
                labels: { color: '#94a3b8', font: { family: 'Inter', size: 11 } }
              },
              tooltip: {
                callbacks: {
                  label: (ctx) => {
                    const val = ctx.parsed !== null && ctx.parsed !== undefined ? ctx.parsed : 0;
                    const total = (ctx.dataset.data as number[]).reduce((a, b) => a + b, 0);
                    const pct = total > 0 ? ((val / total) * 100).toFixed(1) : '0.0';
                    return ` ₹${val.toLocaleString('en-IN', { minimumFractionDigits: 2 })} (${pct}%)`;
                  }
                }
              }
            }
          }
        });
      }
    }
  }
}
