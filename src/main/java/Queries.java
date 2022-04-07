import java.rmi.RemoteException;

public interface Queries extends java.rmi.Remote {

        String createLogin(String loginName, String realName, String password) throws RemoteException;

        String lookupByLogin(String loginName) throws RemoteException;

        String lookupByUUID(String Uuid) throws RemoteException;

        String modifyName(String oldLoginName, String newLoginName, String password) throws RemoteException;

        String deleteUser(String loginName, String password) throws RemoteException;

        String getInfo(String type) throws RemoteException;
}
