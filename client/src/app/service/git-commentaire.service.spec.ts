import { TestBed } from '@angular/core/testing';

import { GitCommentaireService } from './git-commentaire.service';

describe('GitCommentaireService', () => {
  let service: GitCommentaireService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(GitCommentaireService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
