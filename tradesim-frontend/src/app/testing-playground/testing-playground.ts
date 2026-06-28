import { Component } from '@angular/core';
import { Button } from '../shared/components/button/button';

@Component({
  selector: 'app-testing-playground',
  imports: [Button],
  templateUrl: './testing-playground.html',
  styleUrl: './testing-playground.scss',
})
export class TestingPlayground { }
