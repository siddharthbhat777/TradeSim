import { Component } from '@angular/core';
import { Button } from '../shared/components/button/button';
import { Badge } from '../shared/components/badge/badge';

@Component({
  selector: 'app-testing-playground',
  imports: [Button, Badge],
  templateUrl: './testing-playground.html',
  styleUrl: './testing-playground.scss',
})
export class TestingPlayground { }
