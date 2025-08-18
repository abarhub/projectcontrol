import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DetailsProjet } from './details-projet';

describe('DetailsProjet', () => {
  let component: DetailsProjet;
  let fixture: ComponentFixture<DetailsProjet>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DetailsProjet]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DetailsProjet);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
