import {Component, Inject} from '@angular/core';
import {
    MAT_DIALOG_DATA,
    MatDialogActions,
    MatDialogContent,
    MatDialogRef,
    MatDialogTitle
} from '@angular/material/dialog';
import {MatButton} from '@angular/material/button';
import {MatIcon} from '@angular/material/icon';
import {NgClass, NgIf} from '@angular/common';


@Component({
    selector: 'app-confirmation-dialog',
    templateUrl: './confirmation-dialog.component.html',
    imports: [
        MatDialogTitle,
        MatDialogContent,
        MatDialogActions,
        MatButton,
        MatIcon,
        NgClass,
        NgIf
    ]
})
export class ConfirmationDialogComponent {
    constructor(
        public dialogRef: MatDialogRef<ConfirmationDialogComponent>,
        @Inject(MAT_DIALOG_DATA) public data: { title: string; message: string; type: string }
    ) {
    }

    onConfirm(): void {
        this.dialogRef.close(true);
    }

    onCancel(): void {
        this.dialogRef.close(false);
    }

    getIcon(type: string): string {
        switch (type) {
            case 'error':
                return 'error'; // Material icon for error
            case 'notification':
                return 'notifications'; // Material icon for notification
            case 'confirmation':
                return 'help'; // Material icon for confirmation
            default:
                return 'info'; // Default icon
        }
    }

}
