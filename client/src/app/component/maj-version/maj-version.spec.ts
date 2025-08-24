import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MajVersion } from './maj-version';

describe('MajVersionApi', () => {
  let component: MajVersion;
  let fixture: ComponentFixture<MajVersion>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MajVersion]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MajVersion);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
