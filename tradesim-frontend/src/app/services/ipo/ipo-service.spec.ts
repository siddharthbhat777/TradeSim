import { TestBed } from '@angular/core/testing';

import { IpoService } from './ipo-service';

describe('IpoService', () => {
  let service: IpoService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(IpoService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
