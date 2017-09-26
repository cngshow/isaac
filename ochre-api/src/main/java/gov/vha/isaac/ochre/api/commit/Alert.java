/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.vha.isaac.ochre.api.commit;

/**
 *
 * @author kec
 */
public interface Alert extends Comparable<Alert> {
    
    AlertType getAlertType();
    
    String getAlertText();
    
    int getComponentNidForAlert();
    
    Object[] getFixups();

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    default int compareTo(Alert o) {	
        return getAlertType().ordinal() - o.getAlertType().ordinal();
    }
}
