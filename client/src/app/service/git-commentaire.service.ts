import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {LigneGit} from '../entity/ligne-git';

@Injectable({
  providedIn: 'root'
})
export class GitCommentaireService {

  private http = inject(HttpClient);
  private baseUrl = '/api/git-commentaire';

  getGitCommentaire(groupeId: string, nom: string, nbCommit: number): Observable<LigneGit[]> {
    return this.http.get<LigneGit[]>(this.baseUrl + '/' + groupeId + '/' + nom + '?nbCommit=' + nbCommit);
  }

}
