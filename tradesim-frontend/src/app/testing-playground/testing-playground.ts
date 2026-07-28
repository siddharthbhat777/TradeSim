import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Logo } from '../shared/components/logo/logo';
import { ToastService } from '../shared/components/toast/toast.service'; // Added ToastService import
import { Button } from '../shared/components/button/button';
import { Badge } from '../shared/components/badge/badge';
import { Card } from '../shared/components/card/card';

interface DocSection {
  title: string;
  items: { id: string; name: string }[];
}

@Component({
  selector: 'app-testing-playground',
  imports: [CommonModule, Logo, Button, Badge, Card],
  templateUrl: './testing-playground.html',
  styleUrls: ['./testing-playground.scss']
})
export class TestingPlayground {
  // Inject the ToastService
  private toastService = inject(ToastService);

  // Structured Navigation
  navigation: DocSection[] = [
    {
      title: 'Foundations',
      items: [
        { id: 'logo', name: 'Logo' },
        { id: 'colors', name: 'Color Palette' },
        { id: 'typography', name: 'Typography' }
      ]
    },
    {
      title: 'Core Components',
      items: [
        { id: 'button', name: 'Button' },
        { id: 'badge', name: 'Badge' },
        { id: 'card', name: 'Card' }
      ]
    },
    {
      title: 'Form Inputs',
      items: [
        { id: 'dropdown', name: 'Dropdown' },
        { id: 'segmented-control', name: 'Segmented Control' }
      ]
    }
  ];

  // Replace your current colorGroups array with this one:
  colorGroups = [
    {
      group: 'Background & Surface',
      tokens: [
        { name: 'Background', variable: '--background' },
        { name: 'Surface', variable: '--surface' },
        { name: 'Surface Sec', variable: '--surface-secondary' },
      ]
    },
    {
      group: 'Borders & Outlines', // Added this new group!
      tokens: [
        { name: 'Border', variable: '--border' },
        { name: 'Outline BG', variable: '--outline-bg' },
        { name: 'Outline Border', variable: '--outline-border' },
      ]
    },
    {
      group: 'Brand Actions',
      tokens: [
        { name: 'Primary', variable: '--primary' },
        { name: 'Secondary', variable: '--secondary' },
        { name: 'Accent', variable: '--accent' },
      ]
    },
    {
      group: 'Semantic / Feedback',
      tokens: [
        { name: 'Success', variable: '--success' },
        { name: 'Danger', variable: '--danger' },
        { name: 'Warning', variable: '--warning' },
      ]
    },
    {
      group: 'Charts',
      tokens: [
        { name: 'Chart 1', variable: '--chart-1' },
        { name: 'Chart 2', variable: '--chart-2' },
        { name: 'Chart 3', variable: '--chart-3' },
        { name: 'Chart 4', variable: '--chart-4' },
        { name: 'Chart 5', variable: '--chart-5' },
      ]
    }
    // Notice: The Text group is completely removed from here!
  ];

  // Code Snippets
  logoCodeSnippet = `<app-logo size="large" [showText]="true"></app-logo>`;

  // Actions
  scrollTo(id: string): void {
    const element = document.getElementById(id);
    if (element) {
      const y = element.getBoundingClientRect().top + window.scrollY - 80;
      window.scrollTo({ top: y, behavior: 'smooth' });
    }
  }

  async copyCode(code: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(code);
      // Trigger the toast message on success
      this.toastService.success('Copied to clipboard!', { position: 'bottom-right' });
    } catch (err) {
      console.error('Failed to copy code: ', err);
      // Fixed: Using .danger() instead of .error()
      this.toastService.danger('Failed to copy code to clipboard', { position: 'bottom-right' });
    }
  }
}