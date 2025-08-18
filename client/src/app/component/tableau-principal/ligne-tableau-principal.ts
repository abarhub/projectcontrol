export class LigneTableauPrincipal {

  id: string = '';
  nom: string = '';
  groupeId: string = '';
  version: string = '';
  parent: string = '';
  description: string = '';
  infoGitDate: Date | null = null;
  infoGitIdCommit: string = '';
  infoGitBranche: string = '';
  infoGitMessage: string = '';
  dateModification: Date | null = null;
  modules: string[] = [];
  detailModules: Map<string, string> = new Map<string, string>();
  repertoire: string = '';

}
