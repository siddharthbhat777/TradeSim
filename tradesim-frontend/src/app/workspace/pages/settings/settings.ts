import { Component, ChangeDetectionStrategy, signal, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Card } from '../../../shared/components/card/card';
import { CustomInput } from '../../../shared/components/input/input';
import { InputDirective } from '../../../shared/directives/input';
import { Button } from '../../../shared/components/button/button';
import { Dropdown, DropdownOption } from '../../../shared/components/dropdown/dropdown';
import { ToastService } from '../../../shared/components/toast/toast.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, Card, CustomInput, InputDirective, Button, Dropdown],
  templateUrl: './settings.html',
  styleUrl: './settings.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Settings implements OnInit {
  activeTab = signal<'profile' | 'security' | 'preferences' | 'faqs'>('profile');

  private fb = inject(FormBuilder);
  private toastService = inject(ToastService);

  themeOptions: DropdownOption<string>[] = [
    { label: 'System Default', value: 'SYSTEM' },
    { label: 'Light Mode', value: 'LIGHT' },
    { label: 'Dark Mode', value: 'DARK' }
  ];

  countryOptions: DropdownOption<string>[] = [
    { label: 'India', value: 'IN' },
    { label: 'United States', value: 'US' },
    { label: 'United Kingdom', value: 'UK' }
  ];

  bankOptionsMap: Record<string, DropdownOption<string>[]> = {
    'IN': [
      { label: 'HDFC Bank', value: 'HDFC Bank' },
      { label: 'ICICI Bank', value: 'ICICI Bank' },
      { label: 'State Bank of India', value: 'State Bank of India' }
    ],
    'US': [
      { label: 'Chase Bank', value: 'Chase Bank' },
      { label: 'Bank of America', value: 'Bank of America' },
      { label: 'Wells Fargo', value: 'Wells Fargo' }
    ],
    'UK': [
      { label: 'Barclays', value: 'Barclays' },
      { label: 'HSBC', value: 'HSBC' },
      { label: 'Lloyds Bank', value: 'Lloyds Bank' }
    ]
  };

  currentBankOptions = signal<DropdownOption<string>[]>([]);

  profileForm = this.fb.group({
    fullName: [''],
    linkedBankName: [''],
    countryCode: [{ value: '', disabled: true }],
    baseCurrency: [{ value: '', disabled: true }]
  });

  securityForm = this.fb.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', Validators.required],
    confirmPassword: ['', Validators.required]
  });

  preferencesForm = this.fb.group({
    themePreference: ['SYSTEM']
  });

  faqs = signal([
    { question: 'How do I change my base currency?', answer: 'Base currency cannot be changed once the account is created to maintain consistent trading history.' },
    { question: 'What happens when I deactivate my account?', answer: 'Your account will be suspended and you will not be able to log in. You can reactivate it later.' },
    { question: 'Are my trading details secure?', answer: 'Yes, we use industry standard encryption to protect your financial data.' }
  ]);

  expandedFaqIndex = signal<number | null>(null);

  ngOnInit() {
    const userCountry = 'IN';

    this.currentBankOptions.set(this.bankOptionsMap[userCountry] || []);

    this.profileForm.patchValue({
      fullName: 'Siddharth Bhat',
      linkedBankName: 'HDFC Bank',
      countryCode: userCountry,
      baseCurrency: 'INR'
    });
  }

  setTab(tab: 'profile' | 'security' | 'preferences' | 'faqs') {
    this.activeTab.set(tab);
  }

  onSaveProfile() {
    this.toastService.info('Profile update functionality coming soon!');
  }

  onSaveSecurity() {
    if (this.securityForm.invalid) {
      this.securityForm.markAllAsTouched();
      return;
    }
    this.toastService.info('Security update functionality coming soon!');
  }

  onSavePreferences() {
    this.toastService.info('Preferences update functionality coming soon!');
  }

  onDeactivateAccount() {
    this.toastService.warning('Account deactivation functionality coming soon!');
  }

  toggleFaq(index: number) {
    this.expandedFaqIndex.set(this.expandedFaqIndex() === index ? null : index);
  }
}