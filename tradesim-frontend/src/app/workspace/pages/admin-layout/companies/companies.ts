import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Card } from '../../../../shared/components/card/card';
import { Table, TableColumn, TableCellDirective } from '../../../../shared/components/table/table';
import { Badge } from '../../../../shared/components/badge/badge';
import { Button } from '../../../../shared/components/button/button';
import { CustomInput } from '../../../../shared/components/input/input';
import { InputDirective } from '../../../../shared/directives/input';
import { Drawer } from '../../../../shared/components/drawer/drawer';
import { Toggle } from '../../../../shared/components/toggle/toggle';
import { SegmentedControl, SegmentOption } from '../../../../shared/components/segmented-control/segmented-control';
import { Dropdown, DropdownOption } from '../../../../shared/components/dropdown/dropdown';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { AuthService } from '../../../../services/auth/auth-service';
import { CompanyService } from '../../../../services/company/company-service';
import { CompanyResponse } from '../../../../models/company';
import { CountryNamePipe } from '../../../../shared/pipes/country-name-pipe';

@Component({
  selector: 'app-companies',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    Card,
    Table,
    TableCellDirective,
    Badge,
    Button,
    CustomInput,
    InputDirective,
    Drawer,
    Toggle,
    SegmentedControl,
    Dropdown,
    CountryNamePipe
  ],
  templateUrl: './companies.html',
  styleUrl: './companies.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Companies implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly companyService = inject(CompanyService);
  private readonly authService = inject(AuthService);
  private readonly countryNamePipe = new CountryNamePipe();

  readonly allCompanies = signal<CompanyResponse[]>([]);
  readonly isLoading = signal(true);

  readonly searchQuery = signal('');
  readonly statusFilter = signal<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');
  readonly filterCountry = signal<string | null>(null);

  readonly showDrawer = signal(false);
  readonly isOnboarding = signal(false);
  readonly isSubmitting = signal(false);
  readonly isSendingOtp = signal(false);

  readonly statusOptions: SegmentOption<'ALL' | 'ACTIVE' | 'INACTIVE'>[] = [
    { label: 'All', value: 'ALL' },
    { label: 'Active', value: 'ACTIVE' },
    { label: 'Inactive', value: 'INACTIVE' }
  ];

  readonly countryOptions = computed<DropdownOption<string | null>[]>(() => {
    const companies = this.allCompanies();
    const uniqueCountries = Array.from(new Set(companies.map(c => c.country).filter(Boolean))).sort();

    return [
      { label: 'All Countries', value: null },
      ...uniqueCountries.map(c => ({
        label: this.countryNamePipe.transform(c, true),
        value: c
      }))
    ];
  });

  readonly columns = signal<TableColumn<CompanyResponse>[]>([
    { key: 'code', header: 'Code' },
    { key: 'name', header: 'Company Name' },
    { key: 'country', header: 'Country Code' },
    { key: 'status', header: 'Status' }
  ]);

  readonly companyForm = this.fb.nonNullable.group({
    name: ['', Validators.required],
    code: ['', [Validators.required, Validators.pattern(/^[A-Z0-9]+$/)]],
    country: ['', [Validators.required, Validators.pattern(/^[a-zA-Z]{2}$/)]]
  });

  readonly repForm = this.fb.nonNullable.group({
    fullName: ['', Validators.required],
    username: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    linkedBankName: ['', Validators.required],
    countryCode: ['', [Validators.required, Validators.pattern(/^[a-zA-Z]{2}$/)]],
    baseCurrency: ['', [Validators.required, Validators.pattern(/^[a-zA-Z]{3}$/)]],
    otp: ['', Validators.required]
  });

  readonly filteredCompanies = computed(() => {
    let filtered = this.allCompanies();
    const query = this.searchQuery().toLowerCase().trim();
    const status = this.statusFilter();
    const country = this.filterCountry();

    if (query) {
      filtered = filtered.filter(c =>
        c.name.toLowerCase().includes(query) ||
        c.code.toLowerCase().includes(query)
      );
    }

    if (status !== 'ALL') {
      filtered = filtered.filter(c => c.status === status);
    }

    if (country) {
      filtered = filtered.filter(c => c.country === country);
    }

    return filtered;
  });

  ngOnInit(): void {
    this.loadCompanies();
  }

  loadCompanies(): void {
    this.isLoading.set(true);
    this.companyService.getCompanies().subscribe({
      next: (data) => {
        this.allCompanies.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  openCreationDrawer(): void {
    this.companyForm.reset();
    this.repForm.reset();
    this.isOnboarding.set(false);
    this.showDrawer.set(true);
  }

  closeDrawer(): void {
    this.showDrawer.set(false);
  }

  sendOtp(): void {
    if (this.repForm.controls.email.invalid) {
      this.repForm.controls.email.markAsTouched();
      return;
    }

    this.isSendingOtp.set(true);
    this.authService.requestOtp({ email: this.repForm.controls.email.value, purpose: 'REGISTRATION' }).subscribe({
      next: () => {
        this.isSendingOtp.set(false);
        this.toast.success('OTP sent to representative email.');
      },
      error: () => {
        this.isSendingOtp.set(false);
      }
    });
  }

  submitForm(): void {
    if (this.companyForm.invalid) {
      this.companyForm.markAllAsTouched();
      return;
    }

    if (this.isOnboarding() && this.repForm.invalid) {
      this.repForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);

    if (this.isOnboarding()) {
      const company = this.companyForm.getRawValue();
      const representative = this.repForm.getRawValue();

      company.code = company.code.toUpperCase();
      company.country = company.country.toUpperCase();
      representative.countryCode = representative.countryCode.toUpperCase();
      representative.baseCurrency = representative.baseCurrency.toUpperCase();

      const payload = {
        company,
        representative
      };

      this.companyService.onboardCompany(payload).subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.toast.success('Company boarded successfully.');
          this.closeDrawer();
          this.loadCompanies();
        },
        error: () => {
          this.isSubmitting.set(false);
        }
      });
    } else {
      const company = this.companyForm.getRawValue();

      company.code = company.code.toUpperCase();
      company.country = company.country.toUpperCase();

      this.companyService.createCompany(company).subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.toast.success('Company created successfully.');
          this.closeDrawer();
          this.loadCompanies();
        },
        error: () => {
          this.isSubmitting.set(false);
        }
      });
    }
  }

  goToDetails(company: CompanyResponse): void {
    this.router.navigate(['/app/admin/companies', company.id]);
  }
}