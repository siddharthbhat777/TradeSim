import { TestBed } from '@angular/core/testing';

import { DataSyncService } from './data-sync-service';

describe('DataSyncService', () => {
  let service: DataSyncService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(DataSyncService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
