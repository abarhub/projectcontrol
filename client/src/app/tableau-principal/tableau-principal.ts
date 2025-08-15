import {Component} from '@angular/core';
import {ProjetService} from '../service/projet.service';


@Component({
  selector: 'app-tableau-principal',
  imports: [],
  templateUrl: './tableau-principal.html',
  styleUrl: './tableau-principal.scss'
})
export class TableauPrincipal {

  listeProjet: Projet[] = [];

  constructor(private projetService: ProjetService) {
  }


  public test() {
    this.projetService.getProjet().subscribe({
        next: (data) => {
          console.log(data);
          this.listeProjet = data;
        },
        error: (error) => {
          console.error(error);
        }
      }
    )
  }
}
