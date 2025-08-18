import {Component, input} from '@angular/core';
import {DatePipe, KeyValuePipe} from "@angular/common";
import {Projet} from '../../entity/projet';

@Component({
  selector: 'app-details-projet',
    imports: [
        DatePipe,
        KeyValuePipe
    ],
  templateUrl: './details-projet.html',
  styleUrl: './details-projet.scss'
})
export class DetailsProjet {

  projet = input<Projet|null>(null);
  moduleEnfant = input<boolean>(false);

}
