import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SelectRun } from './select-run';

describe('SelectRun', () => {
  let component: SelectRun;
  let fixture: ComponentFixture<SelectRun>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SelectRun]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SelectRun);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
