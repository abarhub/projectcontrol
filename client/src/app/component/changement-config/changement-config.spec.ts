import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChangementConfig } from './changement-config';

describe('ChangementConfig', () => {
  let component: ChangementConfig;
  let fixture: ComponentFixture<ChangementConfig>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChangementConfig]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ChangementConfig);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
