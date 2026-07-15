import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-budgets',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './budgets.component.html',
  styleUrl: './budgets.component.css'
})
export class BudgetsComponent implements OnInit {
  apiService = inject(ApiService);

  budgets: any[] = [];
  expenseCategories: any[] = [];

  // Filter Date
  selectedMonth = new Date().getMonth() + 1;
  selectedYear = new Date().getFullYear();

  monthsList = [
    { value: 1, name: 'January' },
    { value: 2, name: 'February' },
    { value: 3, name: 'March' },
    { value: 4, name: 'April' },
    { value: 5, name: 'May' },
    { value: 6, name: 'June' },
    { value: 7, name: 'July' },
    { value: 8, name: 'August' },
    { value: 9, name: 'September' },
    { value: 10, name: 'October' },
    { value: 11, name: 'November' },
    { value: 12, name: 'December' }
  ];

  yearsList = [2024, 2025, 2026, 2027, 2028];

  // Modal
  showModal = false;
  isEditMode = false;
  budgetForm = {
    id: 0,
    categoryId: '',
    amount: 0,
    month: new Date().getMonth() + 1,
    year: new Date().getFullYear()
  };

  // Category Creation Modal
  showCategoryModal = false;
  catForm = { name: '', type: 'EXPENSE' };

  errorMessage = '';
  modalErrorMessage = '';

  ngOnInit(): void {
    this.loadBudgets();
    this.loadExpenseCategories();
  }

  loadBudgets(): void {
    this.apiService.getBudgets(this.selectedMonth, this.selectedYear).subscribe({
      next: (res) => {
        this.budgets = res;
      },
      error: (err) => {
        this.errorMessage = 'Failed to load budgets.';
        console.error(err);
      }
    });
  }

  loadExpenseCategories(): void {
    this.apiService.getCategories().subscribe({
      next: (res) => {
        // Filter only EXPENSE categories
        this.expenseCategories = res.filter((cat: any) => cat.type === 'EXPENSE');
      },
      error: (err) => console.error('Failed to load categories:', err)
    });
  }

  openAddModal(): void {
    this.isEditMode = false;
    this.budgetForm = {
      id: 0,
      categoryId: '',
      amount: 0,
      month: this.selectedMonth,
      year: this.selectedYear
    };
    this.modalErrorMessage = '';
    this.showModal = true;
  }

  openEditModal(budget: any): void {
    this.isEditMode = true;
    this.budgetForm = {
      id: budget.id,
      categoryId: budget.category ? budget.category.id.toString() : '',
      amount: budget.amount,
      month: budget.month,
      year: budget.year
    };
    this.modalErrorMessage = '';
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
  }

  saveBudget(): void {
    this.modalErrorMessage = '';

    if (this.budgetForm.amount <= 0) {
      this.modalErrorMessage = 'Budget limit must be greater than zero.';
      return;
    }

    if (!this.budgetForm.categoryId) {
      this.modalErrorMessage = 'Please select a category.';
      return;
    }

    const payload = {
      categoryId: Number(this.budgetForm.categoryId),
      amount: this.budgetForm.amount,
      month: Number(this.budgetForm.month),
      year: Number(this.budgetForm.year)
    };

    if (this.isEditMode) {
      this.apiService.updateBudget(this.budgetForm.id, payload).subscribe({
        next: () => {
          this.closeModal();
          this.loadBudgets();
        },
        error: (err) => {
          this.modalErrorMessage = err.error?.message || 'Failed to update budget.';
        }
      });
    } else {
      this.apiService.createBudget(payload).subscribe({
        next: () => {
          this.closeModal();
          this.loadBudgets();
        },
        error: (err) => {
          this.modalErrorMessage = err.error?.message || 'Failed to create budget.';
        }
      });
    }
  }

  deleteBudget(id: number): void {
    if (confirm('Are you sure you want to delete this budget limit?')) {
      this.apiService.deleteBudget(id).subscribe({
        next: () => this.loadBudgets(),
        error: (err) => console.error('Failed to delete budget:', err)
      });
    }
  }

  openCategoryModal(): void {
    this.catForm = { name: '', type: 'EXPENSE' };
    this.showCategoryModal = true;
  }

  closeCategoryModal(): void {
    this.showCategoryModal = false;
  }

  saveCategory(): void {
    if (!this.catForm.name.trim()) return;
    this.apiService.createCategory(this.catForm).subscribe({
      next: () => {
        this.closeCategoryModal();
        this.loadExpenseCategories();
      },
      error: (err) => alert(err.error?.message || 'Failed to create category.')
    });
  }

  // Helpers for Progress Bar calculations
  getPercent(spent: number, limit: number): number {
    if (limit <= 0) return 0;
    return Math.min(Math.round((spent / limit) * 100), 100);
  }

  getProgressBarColorClass(spent: number, limit: number): string {
    if (limit <= 0) return 'bg-cyan';
    const percent = (spent / limit) * 100;
    if (percent >= 100) return 'bg-red';
    if (percent >= 90) return 'bg-orange';
    return 'bg-cyan';
  }
}
