package com.cygnus

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.core.view.iterator
import co.aspirasoft.util.InputUtils.hideKeyboard
import co.aspirasoft.util.InputUtils.isNotBlank
import co.aspirasoft.util.InputUtils.showError
import com.cygnus.core.DashboardActivity
import com.cygnus.dao.Invite
import com.cygnus.dao.InvitesDao
import com.cygnus.model.School
import com.cygnus.model.User
import com.cygnus.tasks.InvitationTask
import com.cygnus.view.EmailsInputDialog
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_school.*

/**
 * SchoolDashboardActivity is the schools' homepage.
 *
 * Purpose of this activity is to provide a UI to the schools
 * which allows them to perform certain administrative tasks.
 *
 * @author saifkhichi96
 * @since 1.0.0
 */
class SchoolDashboardActivity : DashboardActivity() {

    private val invitedStaff = ArrayList<Invite>()
    private val joinedStaff = ArrayList<Invite>()
    var classname: String? = null
    var teachername: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_school)
        setSupportActionBar(toolbar)
        supportActionBar?.title = ""
        Log.e("msg","schoolName----"+schoolId)

        val clslist: MutableList<String> = mutableListOf()
        val teacherlist: MutableList<String> = mutableListOf()
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        val rootRef = FirebaseDatabase.getInstance().reference
        val uidRef = rootRef.child(schoolId)
                .child("classes")

        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (ds in dataSnapshot.getChildren()) {

                    val cls_teacherid = ds.child("teacherId").getValue().toString()

                    //display classname
                    classname = ds.child("name").getValue().toString()
                    teachername = ds.child("teacherId").getValue().toString()
                    clslist.add(classname.toString())
                    teacherlist.add(teachername.toString())

                    admin_class.setAdapter(ArrayAdapter(applicationContext,
                            android.R.layout.select_dialog_item, clslist))


                }
                //Log.e("msggg3",""+ clslist.size);   //gives the value for given keyname

            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.d("msg", databaseError.message) //Don't ignore errors!
            }
        }
        uidRef.addListenerForSingleValueEvent(valueEventListener)

        admin_class.setOnItemClickListener { adapterView, view, i, l ->
            val t_name = admin_class.adapter.getItem(i).toString()
            Log.e("msg", "UNITSSS" + t_name)

            val uidRef1 = rootRef.child(schoolId)
                    .child("classes")

            val valueEventListener1 = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (ds in dataSnapshot.getChildren()) {

                        val cls_teacherid = ds.child("teacherId").getValue().toString()

                        //display classname
                        val c = ds.child("name").getValue().toString()
                        if (t_name.equals(c)) {
                            teachername = ds.child("teachername").getValue().toString()
                            if (teachername.equals("")) {
                                admin_teacher.setText("No Teacher Assigned")

                            } else {
                                admin_teacher.setText(teachername)

                            }
                        }

                    }
                    //Log.e("msggg3",""+ clslist.size);   //gives the value for given keyname

                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.d("msg", databaseError.message) //Don't ignore errors!
                }
            }
            uidRef1.addListenerForSingleValueEvent(valueEventListener1)


        }
    }

    override fun onStart() {
        super.onStart()
        trackSentInvites() // start the live counters
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menu?.iterator()?.forEach { it.icon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP) }
        return true
    }

    /**
     * Displays the signed in school's details.
     */
    override fun updateUI(currentUser: User) {
        if (currentUser is School) {
            schoolName.text = currentUser.name
            schoolEmail.text = currentUser.email
            schoolCode.setImageBitmap(QRGEncoder(currentUser.id, null, QRGContents.Type.TEXT, 512).bitmap)

            showStaffStats(0, 0)
        } else finish()
    }

    fun onManageClassesClicked(v: View) {
        startSecurely(SchoolClassesActivity::class.java, Intent().apply {
            putExtra(CygnusApp.EXTRA_INVITES, joinedStaff)
        })
    }

    fun onManageSubjectsClicked(v: View) {
        startSecurely(SchoolSubjectsActivity::class.java, Intent().apply {
            putExtra(CygnusApp.EXTRA_INVITES, joinedStaff)
        })
    }

    /**
     * Handles clicks on single invite button.
     *
     * Reads an email address from [emailField] and tries generating an invite
     * for this address. A blocking progress box is displayed while the invite
     * is being sent.
     */
    fun onInviteSingleClicked(v: View) {
        if (emailField.isNotBlank()) {
            hideKeyboard()
            val email = emailField.text.toString().trim()
            val progressDialog = ProgressDialog.show(
                    this,
                    getString(R.string.status_invitation_sending),
                    String.format(getString(R.string.status_invitation_progress), email),
                    true
            )

            inviteSingleEmail(email, OnCompleteListener {
                progressDialog.dismiss()
                if (!it.isSuccessful) {
                    emailField.showError(it.exception?.message
                            ?: getString(R.string.error_invitation_failure))
                }
            })
        }
    }

    /**
     * Handles clicks on multiple invites button.
     *
     * Opens a [EmailsInputDialog] where the user can input multiple email
     * addresses in a form. Invites for all valid emails are generated on
     * successful input.
     *
     * A blocking progress box is displayed while the invites are being
     * sent. Status of each sent invite is displayed.
     */
    fun onInviteMultipleClicked(v: View) {
        hideKeyboard()
        EmailsInputDialog(this)
                .setOnEmailsReceivedListener { emails ->
                    inviteMultipleEmails(emails)
                }
                .show()
    }

    /**
     * Handles clicks on invited staff button.
     *
     * Opens the [SchoolTeachersActivity] with a list of staff members who have pending
     * invitations.
     */
    fun onInvitedStaffClicked(v: View) {
        hideKeyboard()
        if (invitedStaff.size > 0) {
            startSecurely(SchoolTeachersActivity::class.java, Intent().apply {
                putExtra(CygnusApp.EXTRA_INVITE_STATUS, getString(R.string.status_invite_pending))
                putExtra(CygnusApp.EXTRA_INVITES, invitedStaff)
            })
        } else {
            Snackbar.make(joinedStaffButton, "No staff members invited yet.", Snackbar.LENGTH_SHORT).show()
        }
    }

    /**
     * Handles clicks on joined staff button.
     *
     * Opens the [SchoolTeachersActivity] with a list of staff members who have accepted
     * their invitations and joined the app.
     */
    fun onJoinedStaffClicked(v: View) {
        hideKeyboard()
        if (joinedStaff.size > 0) {
            startSecurely(SchoolTeachersActivity::class.java, Intent().apply {
                putExtra(CygnusApp.EXTRA_INVITE_STATUS, getString(R.string.status_invite_accepted))
                putExtra(CygnusApp.EXTRA_INVITES, joinedStaff)
            })
        } else {
            Snackbar.make(joinedStaffButton, "No staff members have joined yet.", Snackbar.LENGTH_SHORT).show()
        }
    }

    /**
     * Sends a link to [email] which can be used to create a Teacher account.
     *
     * @param listener Optional callback to listen for completion of invitation task.
     */
    private fun inviteSingleEmail(email: String, listener: OnCompleteListener<Void?>? = null) {
        InvitationTask(this, currentUser.id, email, schoolId)
                .start { task ->
                    if (task.isSuccessful || task.exception == null) {
                        Toast.makeText(this@SchoolDashboardActivity, getString(R.string.status_invitation_sent), Toast.LENGTH_LONG).show()
                        emailField.setText("")
                    } else {
                        emailField.showError("Email ${task.exception?.message
                                ?: "could not be sent"}.")
                    }

                    listener?.let { task.addOnCompleteListener(it) }
                }
    }

    /**
     * Sends links to [emails] which can be used to create Teacher accounts.
     */
    private fun inviteMultipleEmails(emails: List<String>) {
        val invitees = emails.size
        var invited = 1

        val progressDialog = ProgressDialog.show(
                this,
                String.format(getString(R.string.status_invitations_sending), invited, invitees),
                String.format(getString(R.string.status_invitations_progress), invited, invitees),
                true
        )

        var status = ""
        for (email in emails) {
            inviteSingleEmail(email, OnCompleteListener {
                progressDialog.setTitle(String.format(getString(R.string.status_invitations_sending), invited, invitees))

                synchronized(status) {
                    status += if (it.isSuccessful) {
                        "$email invited.\n\n"
                    } else {
                        "$email ${it.exception?.message ?: "already exists"}.\n\n"
                    }
                    synchronized(progressDialog) {
                        progressDialog.setMessage(status)
                    }
                }

                invited += 1
                if (invited > invitees) {
                    progressDialog.setTitle(getString(R.string.status_invitations_sent))
                    progressDialog.setCancelable(true)
                    Handler().postDelayed({ progressDialog.dismiss() }, 5000L)
                }
            })
        }
    }

    /**
     * Displays number of invited and joined staff members.
     */
    private fun showStaffStats(invited: Int, joined: Int) {
        invitedStaffButton.text = String.format(getString(R.string.label_staff_invited), invited)
        joinedStaffButton.text = String.format(getString(R.string.label_staff_joined), joined)
    }

    /**
     * Listens for changes in status of sent invites.
     *
     * Monitors all sent invites and displays the number of accepted
     * and pending invites in realtime.
     */
    private fun trackSentInvites() {
        InvitesDao.getAll(currentUser.id, OnSuccessListener {
            invitedStaff.clear()
            joinedStaff.clear()
            it.orEmpty().forEach { invite ->
                if (invite.sender == schoolId) {
                    when (invite.status) {
                        CygnusApp.STATUS_INVITE_PENDING -> invitedStaff.add(invite)
                        else -> joinedStaff.add(invite)
                    }
                }
            }
            showStaffStats(invitedStaff.size, joinedStaff.size)
        })
    }

}