import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit {
  apiService = inject(ApiService);
  router = inject(Router);

  userProfile: any = null;
  
  profileForm = {
    firstName: '',
    lastName: ''
  };

  pwdForm = {
    oldPassword: '',
    newPassword: '',
    confirmNewPassword: ''
  };

  // Status Alerts
  profileSuccess = '';
  profileError = '';
  pwdSuccess = '';
  pwdError = '';
  
  loadingProfile = false;
  loadingPwd = false;

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.apiService.getProfile().subscribe({
      next: (res) => {
        this.userProfile = res;
        this.profileForm.firstName = res.firstName;
        this.profileForm.lastName = res.lastName;
      },
      error: (err) => console.error('Failed to load profile:', err)
    });
  }

  updateProfile(): void {
    this.profileSuccess = '';
    this.profileError = '';
    
    if (!this.profileForm.firstName.trim() || !this.profileForm.lastName.trim()) {
      this.profileError = 'First and Last name are required.';
      return;
    }

    this.loadingProfile = true;
    this.apiService.updateProfile(this.profileForm).subscribe({
      next: (res) => {
        this.profileSuccess = 'Profile details updated successfully.';
        this.loadingProfile = false;
        this.loadProfile();
      },
      error: (err) => {
        this.profileError = err.error?.message || 'Failed to update profile details.';
        this.loadingProfile = false;
      }
    });
  }

  changePassword(): void {
    this.pwdSuccess = '';
    this.pwdError = '';

    if (!this.pwdForm.oldPassword || !this.pwdForm.newPassword) {
      this.pwdError = 'All password fields are required.';
      return;
    }

    if (this.pwdForm.newPassword !== this.pwdForm.confirmNewPassword) {
      this.pwdError = 'New passwords do not match.';
      return;
    }

    if (this.pwdForm.newPassword.length < 6) {
      this.pwdError = 'New password must be at least 6 characters.';
      return;
    }

    this.loadingPwd = true;
    const payload = {
      oldPassword: this.pwdForm.oldPassword,
      newPassword: this.pwdForm.newPassword
    };

    this.apiService.changePassword(payload).subscribe({
      next: () => {
        this.pwdSuccess = 'Password changed successfully.';
        this.pwdForm = { oldPassword: '', newPassword: '', confirmNewPassword: '' };
        this.loadingPwd = false;
      },
      error: (err) => {
        this.pwdError = err.error?.message || 'Failed to change password. Double check old password.';
        this.loadingPwd = false;
      }
    });
  }

  exportTransactions(): void {
    this.apiService.downloadTransactionsCsv().subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'transactions.csv';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        alert('Failed to export transactions.');
        console.error(err);
      }
    });
  }

  deactivateAccount(): void {
    const confirmation = confirm(
      'WARNING: Are you sure you want to deactivate your account? This action is permanent, and you will be logged out immediately.'
    );
    
    if (confirmation) {
      this.apiService.deactivateAccount().subscribe({
        next: () => {
          alert('Account deactivated. You are now logged out.');
          this.apiService.logout();
          this.router.navigate(['/login']);
        },
        error: (err) => {
          alert(err.error?.message || 'Failed to deactivate account.');
        }
      });
    }
  }
}
