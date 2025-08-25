import { TestBed } from '@angular/core/testing';

import { ChangementConfigService } from './changement-config.service';

describe('ChangementConfigService', () => {
  let service: ChangementConfigService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ChangementConfigService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
