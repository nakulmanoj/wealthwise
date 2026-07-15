import { Component, ElementRef, inject, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-investments',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './investments.component.html',
  styleUrl: './investments.component.css'
})
export class InvestmentsComponent implements OnInit {
  apiService = inject(ApiService);

  investments: any[] = [];
  
  // Aggregates
  totalValue = 0;
  totalCost = 0;
  totalProfitLoss = 0;
  totalProfitPercent = 0;

  @ViewChild('allocationCanvas') allocationCanvas!: ElementRef<HTMLCanvasElement>;
  allocationChart: any;

  // Modal
  showModal = false;
  isEditMode = false;
  invForm = {
    id: 0,
    symbol: '',
    name: '',
    quantity: 0,
    averageBuyPrice: 0
  };

  errorMessage = '';
  modalErrorMessage = '';
  loading = false;

  ngOnInit(): void {
    this.loadPortfolio();
  }

  loadPortfolio(): void {
    this.loading = true;
    this.apiService.getPortfolio().subscribe({
      next: (res) => {
        this.investments = res;
        this.calculateAggregates();
        this.loading = false;
        // Wait for template to render
        setTimeout(() => this.renderAllocationChart(), 100);
      },
      error: (err) => {
        this.errorMessage = 'Failed to load portfolio.';
        this.loading = false;
        console.error(err);
      }
    });
  }

  renderAllocationChart(): void {
    if (!this.investments || this.investments.length === 0) return;
    if (!this.allocationCanvas?.nativeElement) return;

    if (this.allocationChart) this.allocationChart.destroy();

    const symbols = this.investments.map(inv => inv.symbol);
    const values  = this.investments.map(inv => Number(inv.currentValue));

    const ctx = this.allocationCanvas.nativeElement.getContext('2d');
    if (ctx) {
      this.allocationChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
          labels: symbols,
          datasets: [{
            data: values,
            backgroundColor: [
              '#06b6d4', '#8b5cf6', '#ec4899', '#f59e0b',
              '#10b981', '#ef4444', '#3b82f6', '#14b8a6', '#64748b'
            ],
            borderWidth: 1,
            borderColor: 'rgba(255, 255, 255, 0.1)'
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
                  return ` ${ctx.label}: ₹${val.toLocaleString('en-IN', { minimumFractionDigits: 2 })} (${pct}%)`;
                }
              }
            }
          }
        }
      });
    }
  }

  calculateAggregates(): void {
    this.totalCost = this.investments.reduce((acc, inv) => acc + inv.totalCost, 0);
    this.totalValue = this.investments.reduce((acc, inv) => acc + inv.currentValue, 0);
    this.totalProfitLoss = this.totalValue - this.totalCost;

    if (this.totalCost > 0) {
      this.totalProfitPercent = (this.totalProfitLoss / this.totalCost) * 100;
    } else {
      this.totalProfitPercent = 0;
    }
  }

  openAddModal(): void {
    this.isEditMode = false;
    this.invForm = {
      id: 0,
      symbol: '',
      name: '',
      quantity: 0,
      averageBuyPrice: 0
    };
    this.modalErrorMessage = '';
    this.showModal = true;
  }

  openEditModal(inv: any): void {
    this.isEditMode = true;
    this.invForm = {
      id: inv.id,
      symbol: inv.symbol,
      name: inv.name,
      quantity: inv.quantity,
      averageBuyPrice: inv.averageBuyPrice
    };
    this.modalErrorMessage = '';
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
  }

  saveInvestment(): void {
    this.modalErrorMessage = '';

    if (!this.invForm.symbol.trim()) {
      this.modalErrorMessage = 'Symbol is required.';
      return;
    }

    if (!this.invForm.name.trim()) {
      this.modalErrorMessage = 'Holding name is required.';
      return;
    }

    if (this.invForm.quantity <= 0) {
      this.modalErrorMessage = 'Quantity must be greater than zero.';
      return;
    }

    if (this.invForm.averageBuyPrice <= 0) {
      this.modalErrorMessage = 'Average buy price must be greater than zero.';
      return;
    }

    const payload = {
      symbol: this.invForm.symbol.trim().toUpperCase(),
      name: this.invForm.name.trim(),
      quantity: this.invForm.quantity,
      averageBuyPrice: this.invForm.averageBuyPrice
    };

    if (this.isEditMode) {
      this.apiService.updateInvestmentManual(this.invForm.id, payload).subscribe({
        next: () => {
          this.closeModal();
          this.loadPortfolio();
        },
        error: (err) => {
          this.modalErrorMessage = err.error?.message || 'Failed to update holding.';
        }
      });
    } else {
      this.apiService.addInvestment(payload).subscribe({
        next: () => {
          this.closeModal();
          this.loadPortfolio();
        },
        error: (err) => {
          this.modalErrorMessage = err.error?.message || 'Failed to add holding.';
        }
      });
    }
  }

  deleteInvestment(id: number): void {
    if (confirm('Are you sure you want to sell/delete this asset from your portfolio?')) {
      this.apiService.deleteInvestment(id).subscribe({
        next: () => this.loadPortfolio(),
        error: (err) => console.error('Failed to remove asset:', err)
      });
    }
  }
}
