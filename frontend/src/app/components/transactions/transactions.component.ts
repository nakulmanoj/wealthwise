import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './transactions.component.html',
  styleUrl: './transactions.component.css'
})
export class TransactionsComponent implements OnInit {
  apiService = inject(ApiService);

  // Data lists
  transactions: any[] = [];
  categories: any[] = [];

  // Pagination & Filtering
  totalPages = 0;
  totalElements = 0;
  selectedDate = '';
  filters = {
    startDate: '',
    endDate: '',
    type: '',
    categoryId: '',
    description: '',
    page: 0,
    size: 10,
    sort: 'date,desc'
  };

  // Modals
  showModal = false;
  isEditMode = false;
  txForm = {
    id: 0,
    amount: 0,
    date: new Date().toISOString().split('T')[0],
    description: '',
    categoryId: '',
    type: 'EXPENSE'
  };

  // Custom Category Creation
  showCategoryModal = false;
  catForm = {
    name: '',
    type: 'EXPENSE'
  };

  errorMessage = '';
  modalErrorMessage = '';

  ngOnInit(): void {
    this.loadTransactions();
    this.loadCategories();
  }

  loadTransactions(): void {
    this.apiService.searchTransactions(this.filters).subscribe({
      next: (res) => {
        this.transactions = res.content;
        this.totalPages = res.totalPages;
        this.totalElements = res.totalElements;
      },
      error: (err) => {
        this.errorMessage = 'Failed to load transactions.';
        console.error(err);
      }
    });
  }

  loadCategories(): void {
    this.apiService.getCategories().subscribe({
      next: (res) => {
        this.categories = res;
      },
      error: (err) => console.error('Failed to load categories:', err)
    });
  }

  /** Returns categories that match the currently-selected transaction type in the form. */
  get filteredCategories(): any[] {
    if (!this.txForm.type) return this.categories;
    return this.categories.filter(c => c.type === this.txForm.type);
  }

  /** When the user changes the Type in the modal, reset the category so the dropdown refreshes. */
  onTypeChange(): void {
    this.txForm.categoryId = '';
  }

  applyFilters(): void {
    this.filters.page = 0;
    this.loadTransactions();
  }

  onDateChange(): void {
    if (this.selectedDate) {
      this.filters.startDate = this.selectedDate;
      this.filters.endDate = this.selectedDate;
    } else {
      this.filters.startDate = '';
      this.filters.endDate = '';
    }
    this.applyFilters();
  }

  resetFilters(): void {
    this.selectedDate = '';
    this.filters = {
      startDate: '',
      endDate: '',
      type: '',
      categoryId: '',
      description: '',
      page: 0,
      size: 10,
      sort: 'date,desc'
    };
    this.loadTransactions();
  }

  changePage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.filters.page = page;
      this.loadTransactions();
    }
  }

  openAddModal(): void {
    this.isEditMode = false;
    this.txForm = {
      id: 0,
      amount: 0,
      date: new Date().toISOString().split('T')[0],
      description: '',
      categoryId: '',
      type: 'EXPENSE'
    };
    this.modalErrorMessage = '';
    this.showModal = true;
  }

  openEditModal(tx: any): void {
    this.isEditMode = true;
    this.txForm = {
      id: tx.id,
      amount: tx.amount,
      date: tx.date,
      description: tx.description,
      categoryId: tx.category ? tx.category.id.toString() : '',
      type: tx.type
    };
    this.modalErrorMessage = '';
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
  }

  saveTransaction(): void {
    this.modalErrorMessage = '';

    if (this.txForm.amount <= 0) {
      this.modalErrorMessage = 'Amount must be greater than zero.';
      return;
    }

    if (!this.txForm.categoryId) {
      this.modalErrorMessage = 'Please select a category.';
      return;
    }

    const payload = {
      amount: this.txForm.amount,
      date: this.txForm.date,
      description: this.txForm.description,
      categoryId: Number(this.txForm.categoryId),
      type: this.txForm.type
    };

    if (this.isEditMode) {
      this.apiService.updateTransaction(this.txForm.id, payload).subscribe({
        next: () => {
          this.closeModal();
          this.loadTransactions();
        },
        error: (err) => {
          this.modalErrorMessage = err.error?.message || 'Failed to update transaction.';
        }
      });
    } else {
      this.apiService.createTransaction(payload).subscribe({
        next: () => {
          this.closeModal();
          this.loadTransactions();
        },
        error: (err) => {
          this.modalErrorMessage = err.error?.message || 'Failed to create transaction.';
        }
      });
    }
  }

  deleteTransaction(id: number): void {
    if (confirm('Are you sure you want to delete this transaction?')) {
      this.apiService.deleteTransaction(id).subscribe({
        next: () => this.loadTransactions(),
        error: (err) => console.error('Failed to delete transaction:', err)
      });
    }
  }

  // Quick Category Modal helpers
  openCategoryModal(): void {
    this.catForm = { name: '', type: this.txForm.type || 'EXPENSE' };
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
        this.loadCategories();
      },
      error: (err) => alert(err.error?.message || 'Failed to create custom category.')
    });
  }
}
