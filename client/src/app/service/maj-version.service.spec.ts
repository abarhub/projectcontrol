import { TestBed } from '@angular/core/testing';

import { MajVersionService } from './maj-version.service';

describe('MajVersionService', () => {
  let service: MajVersionService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(MajVersionService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
