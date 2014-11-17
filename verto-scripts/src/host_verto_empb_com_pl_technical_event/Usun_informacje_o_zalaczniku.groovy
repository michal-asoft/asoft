package host_verto_empb_com_pl_technical_event;

import pl.com.stream.verto.cmm.attachement.server.pub.main.AttachementDto
import pl.com.stream.verto.cmm.attribute.server.pub.value.AttributeValueService

class Usun_informacje_o_zalaczniku extends pl.com.stream.next.asen.common.groovy.TechnicalEventScriptEnv {
    def script() {

        AttachementDto attachementDto = event.getMethodParameters()[0];
        AttributeValueService attributeValueService = context.getService(AttributeValueService.class);

        def idAttachementSubject = attachementDto.idAttachementSubject;

        Long idAttributeDef = 101102L;
        Boolean isAttachements = true;

        String delHql ="Select a.hasAttachments from AttachementSubject a where a.idAttachementSubject =:idAttachementSubject";

        List<Long> idsAttach = dm.executeQuery(delHql, ["idAttachementSubject":idAttachementSubject]);

        if (idsAttach.size()>0) {
            isAttachements = idsAttach.get(0);

            /* if (hasAttachements == 0) {
             isAttachements = false;
             }*/
        }


        List<String> hqlList =
                [
                    "Select s.attributeSubject.idAttributeSubject from SaleOrderDocument s where s.attachementSubject.idAttachementSubject =:idAttachementSubject",
                    "Select s.attributeSubject.idAttributeSubject from SaleDocument s where s.attachementSubject.idAttachementSubject =:idAttachementSubject",
                    "Select s.attributeSubject.idAttributeSubject from SaleRequestDocument s where s.attachementSubject.idAttachementSubject =:idAttachementSubject",
                    "Select s.attributeSubject.idAttributeSubject from PurchaseOrderDocument s where s.attachementSubject.idAttachementSubject =:idAttachementSubject"
                ];

        for (hql in hqlList) {
            List<Long> ids = dm.executeQuery(hql, ["idAttachementSubject":idAttachementSubject]);


            if (ids.size()>0) {
                Long idAttributeSubject = ids.get(0);
                attributeValueService.insertAttributeValueBySubjectAndDef(idAttributeSubject, idAttributeDef, isAttachements);
            }
        }
    }
}