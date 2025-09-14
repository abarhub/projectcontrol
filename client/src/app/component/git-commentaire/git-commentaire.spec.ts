import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GitCommentaire } from './git-commentaire';

describe('GitCommentaire', () => {
  let component: GitCommentaire;
  let fixture: ComponentFixture<GitCommentaire>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GitCommentaire]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GitCommentaire);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
