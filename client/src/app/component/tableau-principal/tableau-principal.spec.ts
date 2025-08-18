import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TableauPrincipal } from './tableau-principal';

describe('TableauPrincipal', () => {
  let component: TableauPrincipal;
  let fixture: ComponentFixture<TableauPrincipal>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TableauPrincipal]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TableauPrincipal);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
