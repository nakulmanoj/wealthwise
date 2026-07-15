import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  apiService = inject(ApiService);
  router = inject(Router);

  details = {
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    confirmPassword: ''
  };
  errorMessage = '';
  successMessage = '';
  loading = false;

  onSubmit(): void {
    this.errorMessage = '';
    this.successMessage = '';
    
    if (this.details.password !== this.details.confirmPassword) {
      this.errorMessage = 'Passwords do not match.';
      return;
    }

    this.loading = true;
    const { confirmPassword, ...registerPayload } = this.details;
    this.apiService.register(registerPayload).subscribe({
      next: () => {
        this.successMessage = 'Registration successful! Redirecting to login...';
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 1500);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Registration failed. Try again.';
        this.loading = false;
      }
    });
  }
}
