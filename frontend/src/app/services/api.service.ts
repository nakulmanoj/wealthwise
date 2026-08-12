import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private http = inject(HttpClient);
  private baseUrl = 'https://wealthwise-backend.onrender.com/api';

  // --- AUTHENTICATION ---
  login(credentials: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/auth/login`, credentials).pipe(
      tap((res: any) => {
        if (res && res.token) {
          localStorage.setItem('jwt_token', res.token);
          localStorage.setItem('user_email', res.email || '');
          localStorage.setItem('user_name', (res.firstName || '') + ' ' + (res.lastName || ''));
        }
      })
    );
  }

  register(details: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/auth/register`, details);
  }

  logout(): void {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('user_email');
    localStorage.removeItem('user_name');
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem('jwt_token');
  }

  getUserName(): string {
    return localStorage.getItem('user_name') || 'User';
  }

  getUserEmail(): string {
    return localStorage.getItem('user_email') || '';
  }

  // --- USER PROFILE ---
  getProfile(): Observable<any> {
    return this.http.get(`${this.baseUrl}/users/profile`);
  }

  updateProfile(data: any): Observable<any> {
    return this.http.put(`${this.baseUrl}/users/profile`, data).pipe(
      tap((res: any) => {
        if (res) {
          localStorage.setItem('user_name', (res.firstName || '') + ' ' + (res.lastName || ''));
        }
      })
    );
  }

  changePassword(data: any): Observable<any> {
    return this.http.put(`${this.baseUrl}/users/change-password`, data);
  }

  deactivateAccount(): Observable<any> {
    return this.http.put(`${this.baseUrl}/users/deactivate`, {});
  }

  // --- CATEGORIES ---
  getCategories(): Observable<any> {
    return this.http.get(`${this.baseUrl}/categories`);
  }

  createCategory(data: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/categories`, data);
  }

  // --- TRANSACTIONS ---
  searchTransactions(filters: any): Observable<any> {
    let params = new HttpParams();
    if (filters.startDate) params = params.set('startDate', filters.startDate);
    if (filters.endDate) params = params.set('endDate', filters.endDate);
    if (filters.type) params = params.set('type', filters.type);
    if (filters.categoryId) params = params.set('categoryId', filters.categoryId.toString());
    if (filters.description) params = params.set('description', filters.description);
    if (filters.page !== undefined) params = params.set('page', filters.page.toString());
    if (filters.size !== undefined) params = params.set('size', filters.size.toString());
    if (filters.sort) params = params.set('sort', filters.sort);

    return this.http.get(`${this.baseUrl}/transactions`, { params });
  }

  createTransaction(data: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/transactions`, data);
  }

  updateTransaction(id: number, data: any): Observable<any> {
    return this.http.put(`${this.baseUrl}/transactions/${id}`, data);
  }

  deleteTransaction(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/transactions/${id}`);
  }

  // --- BUDGETS ---
  getBudgets(month: number, year: number): Observable<any> {
    let params = new HttpParams()
      .set('month', month.toString())
      .set('year', year.toString());
    return this.http.get(`${this.baseUrl}/budgets`, { params });
  }

  createBudget(data: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/budgets`, data);
  }

  updateBudget(id: number, data: any): Observable<any> {
    return this.http.put(`${this.baseUrl}/budgets/${id}`, data);
  }

  deleteBudget(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/budgets/${id}`);
  }

  // --- INVESTMENTS ---
  getPortfolio(): Observable<any> {
    return this.http.get(`${this.baseUrl}/investments`);
  }

  addInvestment(data: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/investments`, data);
  }

  updateInvestmentManual(id: number, data: any): Observable<any> {
    return this.http.put(`${this.baseUrl}/investments/${id}`, data);
  }

  deleteInvestment(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/investments/${id}`);
  }

  // --- DASHBOARD ---
  getDashboardData(): Observable<any> {
    return this.http.get(`${this.baseUrl}/dashboard`);
  }

  // --- REPORTS ---
  getMonthlyReport(month: number, year: number): Observable<any> {
    let params = new HttpParams()
      .set('month', month.toString())
      .set('year', year.toString());
    return this.http.get(`${this.baseUrl}/reports/monthly`, { params });
  }

  downloadTransactionsCsv(): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/reports/export`, { responseType: 'blob' });
  }
}
