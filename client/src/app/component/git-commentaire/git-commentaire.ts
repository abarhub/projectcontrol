import {Component, ElementRef, inject, ViewChild} from '@angular/core';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {Modal} from 'bootstrap';
import {GitCommentaireService} from '../../service/git-commentaire.service';
import {ToasterService} from '../../service/toaster.service';
import {LigneGit} from '../../entity/ligne-git';

@Component({
  selector: 'app-git-commentaire',
  imports: [
    ReactiveFormsModule
  ],
  templateUrl: './git-commentaire.html',
  styleUrl: './git-commentaire.scss'
})
export class GitCommentaire {


  @ViewChild('toastEl', {static: true}) majVersionEl!: ElementRef;

  private majVersionModal!: Modal;

  myForm: FormGroup;

  private groupeId: string = "";
  private nomProjet: string = "";

  messages: LigneGit [] = [];

  chargementMessages: boolean = false;

  private toasterService = inject(ToasterService);

  constructor(private fb: FormBuilder, private gitCommentaireService: GitCommentaireService) {
    this.myForm = this.fb.group({
      nbCommit: ['', Validators.required]
    });
  }

  public show(groupeId: string, nomProjet: string) {
    this.groupeId = groupeId;
    this.nomProjet = nomProjet;
    this.majVersionModal = new Modal(this.majVersionEl.nativeElement);
    this.majVersionModal.show();
  }

  enregistrer($event: MouseEvent) {
    let nbCommit = this.myForm.controls['nbCommit'].value;
    if (nbCommit) {
      let nbCommitInt = parseInt(nbCommit);
      if(nbCommitInt>=0) {
        this.chargementMessages = true;
        this.gitCommentaireService.getGitCommentaire(this.groupeId, this.nomProjet, nbCommitInt)
          .subscribe({
            next: (data) => {
              this.messages = data;
            },
            error: (error) => {
              console.error(error);
              this.toasterService.show("Erreur pour liste les modifications");
              this.chargementMessages = false;
            },
            complete: () => {
              this.chargementMessages = false;
            }
          })
      }
    }
  }
}
